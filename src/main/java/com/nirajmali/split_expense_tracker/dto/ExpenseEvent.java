package com.nirajmali.split_expense_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEvent {

    private String eventType;
    private Long groupId;
    private Long triggeredByUserId;
    private LocalDateTime occurredAt;

}
