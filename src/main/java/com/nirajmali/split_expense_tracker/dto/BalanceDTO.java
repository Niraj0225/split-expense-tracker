package com.nirajmali.split_expense_tracker.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class BalanceDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBalance{
        private Long userId;
        private String userName;
        private BigDecimal netAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupBalanceResponse{
        private Long groupId;
        private String groupName;
        private List<UserBalance> balances;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettleUpRequest {

        @NotNull(message = "Group id is required")
        private Long groupId;

        @NotNull(message = "Paid by user id is required")
        private Long paidByUserId;

        @NotNull(message = "Paid to user id is required")
        private Long paidToUserId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementResponse {
        private String message;
        private String paidBy;
        private String paidTo;
        private BigDecimal amount;
    }
}
