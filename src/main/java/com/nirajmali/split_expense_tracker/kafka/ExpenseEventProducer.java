package com.nirajmali.split_expense_tracker.kafka;

import com.nirajmali.split_expense_tracker.dto.ExpenseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper =
            JsonMapper.builder().build();

    @Value("${app.kafka.topic.expense-events}")
    private String topic;

    public void publishExpenseEvent(ExpenseEvent event) {
        String key = String.valueOf(event.getGroupId());
        String jsonValue = objectMapper.writeValueAsString(event);

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, key, jsonValue);

        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to publish event for group {}: {}",
                        event.getGroupId(), exception.getMessage());
            } else {
                log.info("Published event for group {} to partition {}",
                        event.getGroupId(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}