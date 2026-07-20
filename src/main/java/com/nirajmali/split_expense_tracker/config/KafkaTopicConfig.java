package com.nirajmali.split_expense_tracker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.expense-events}")
    private String expenseEventsTopic;

    @Bean
    public NewTopic expenseEventsTopic(){
        return TopicBuilder.name(expenseEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
