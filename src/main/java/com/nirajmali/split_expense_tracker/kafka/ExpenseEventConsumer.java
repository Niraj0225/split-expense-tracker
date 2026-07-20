package com.nirajmali.split_expense_tracker.kafka;

import com.nirajmali.split_expense_tracker.dto.ExpenseEvent;
import com.nirajmali.split_expense_tracker.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventConsumer {

    private final BalanceService balanceService;

    private final ObjectMapper objectMapper =
            JsonMapper.builder().build();

    @KafkaListener(
            topics = "${app.kafka.topic.expense-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        try {
            ExpenseEvent event = objectMapper.readValue(
                    message, ExpenseEvent.class);

            log.info("Received event: type={}, groupId={}",
                    event.getEventType(),
                    event.getGroupId());

            balanceService.refreshGroupBalanceCache(
                    event.getGroupId());

            log.info("Successfully refreshed balance cache " +
                    "for group {}", event.getGroupId());
        } catch (Exception e) {
            log.error("Failed to process event: {}",
                    e.getMessage());
        }
    }
}