# Split Expense Tracker

A Splitwise-style expense-sharing backend where a group of people can log shared expenses, split them by three different methods, and see exactly who owes whom — with balance calculations offloaded to an async Kafka pipeline and cached in Redis for fast reads.

Built end-to-end with Java, Spring Boot, MySQL, Kafka, and Redis.

## Why this project exists

Most CRUD-only projects don't give you a real reason to reach for Kafka or Redis. This one does — every design decision below solves an actual problem that shows up the moment a group has more than a handful of expenses.

## The core problem

Figuring out "who owes whom" in a group means summing every expense share and every settlement ever recorded for that group. Doing that synchronously, on every single balance check, gets slow as history grows — and doing it inside the "add expense" request makes that endpoint slower for no good reason. The person adding an expense doesn't need to wait for balance math to finish before getting a response.

## How it's solved

1. `POST /api/expenses` saves the expense to MySQL, then publishes an event to a Kafka topic (`expense-events-topic`) and returns immediately. The caller isn't blocked on balance recalculation.
2. A Kafka consumer picks up the event asynchronously, deletes the stale cached balance, and recalculates the affected group's balance from MySQL.
3. The recalculated balance is cached in Redis (`balance:group:{groupId}`, 10-minute TTL).
4. `GET /api/balance/{groupId}` reads from the Redis cache when present and only recomputes from MySQL on a cache miss.
5. Settling up follows the same pattern — the cache is invalidated so the next read is always fresh.

```
Client ──> Spring Boot API ──> MySQL (source of truth)
                  │
                  ├──publish──> Kafka topic (expense-events-topic)
                  │                    │
                  │           ExpenseEventConsumer
                  │                    │
                  │           recompute balance from MySQL
                  │                    │
                  └──cache────> Redis (balance:group:{groupId})
```

## Features

- **Auth** — JWT-based register/login, BCrypt password hashing, stateless session management
- **Groups** — create groups, add members, paginated group listing
- **Expenses** — three split strategies:
  - `EQUAL` — split evenly, with the last member absorbing any rounding remainder so shares always sum exactly to the total
  - `EXACT` — caller specifies the exact amount per person (validated to sum to the total)
  - `PERCENTAGE` — caller specifies a percentage per person (validated to sum to 100)
- **Balances** — net balance per member, computed from expense shares minus settlements
- **Settlements** — record a payment between two members, which invalidates the cached balance
- **Async balance recalculation** — Kafka producer/consumer pipeline described above
- **Redis-backed balance cache** — with graceful degradation: if Redis is unreachable, the app falls back to computing directly from MySQL instead of failing the request

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4 |
| Security | Spring Security, JWT |
| Persistence | Spring Data JPA, Hibernate, MySQL |
| Messaging | Apache Kafka |
| Caching | Redis |
| Build | Maven |

## API overview

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create an account |
| POST | `/api/auth/login` | Log in, receive a JWT |
| POST | `/api/groups` | Create a group |
| GET | `/api/groups/user/my-groups` | List the logged-in user's groups (paginated) |
| GET | `/api/groups/{groupId}` | Get group details |
| POST | `/api/expenses` | Add an expense (EQUAL / EXACT / PERCENTAGE split) |
| GET | `/api/expenses/group/{groupId}` | List a group's expenses (paginated) |
| GET | `/api/balance/{groupId}` | Get the group's balances (Redis-cached) |
| POST | `/api/settlements` | Record a settle-up payment |

## Data model

- **User** — account with name, email, hashed password
- **Group** — many-to-many with User via a `group_members` join table; has a creator
- **Expense** — belongs to a group, has a payer, an amount, and a split type
- **ExpenseShare** — one row per member per expense, storing exactly how much that member owes for that expense
- **Settlement** — records a payment from one member to another within a group

## Running it locally

### Prerequisites

- Java 17+, Maven
- MySQL running on `localhost:3306`
- Kafka running on `localhost:9092`
- Redis running on `localhost:6379`

The easiest way to get Kafka and Redis running locally is Docker:

```bash
docker run -d --name redis -p 6379:6379 redis:7

docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
  -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
  -e CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk \
  apache/kafka:latest
```

### Configuration

Database password and JWT secret are externalized via environment variables with local-dev fallbacks, so the app runs out of the box with no extra setup:

```yaml
password: ${DB_PASSWORD:root}
secret: ${JWT_SECRET:...}
```

To override for your own environment, set `DB_PASSWORD` and `JWT_SECRET` as environment variables before running.

### Run

```bash
mvn spring-boot:run
```

On first run, Hibernate creates all tables automatically (`ddl-auto: update`), and the Kafka topic is created automatically via `KafkaTopicConfig`.

The API starts on `http://localhost:8080`.

## Notable implementation details

- **Lazy-loaded JPA relationships across an async boundary.** The Kafka consumer runs on a separate thread from the original HTTP request, so lazily-loaded collections (like a group's members) have no Hibernate session available by the time the consumer touches them. Fixed by scoping a fresh `@Transactional` session to the consumer's balance-recalculation method.
- **Exact decimal arithmetic for money.** All monetary fields use `BigDecimal`, never `double` — floating point cannot represent decimal fractions exactly, which is unacceptable for financial calculations.
- **Rounding remainder handling in EQUAL splits.** Dividing an odd amount evenly (e.g. ₹100 / 3) doesn't produce a clean result. The last member in the split absorbs the rounding remainder so the shares always sum to exactly the original amount — never a paisa more or less.
- **Cache-aside pattern with graceful degradation.** Redis is a performance optimization, not a dependency the app can't function without — if a cache read or write fails, the app logs it and falls back to computing directly from MySQL rather than failing the request.

## Status

Actively developed as a portfolio project to demonstrate practical use of Kafka and Redis in a real, explainable use case, alongside a from-scratch Spring Security + JWT implementation.
