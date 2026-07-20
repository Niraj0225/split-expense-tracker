package com.nirajmali.split_expense_tracker.dto;

import com.fasterxml.jackson.annotation.JsonDeserializeAs;
import com.nirajmali.split_expense_tracker.entity.SplitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExpenseDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateExpenseRequest{

        @NotNull(message = "Group id is required")
        private Long groupId;

        @NotBlank(message = "Description is required")
        private String description;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        private BigDecimal amount;

        @NotNull(message = "paid by user id is required")
        private Long paidByUserId;

        @NotNull(message = "split type is required")
        private SplitType splitType;

        private Map<String, BigDecimal> customShares;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareDetail{
        private Long userId;
        private String userName;
        private BigDecimal shareAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseResponse{
        private Long id;
        private String description;
        private BigDecimal amount;
        private String paidByName;
        private SplitType splitType;
        private LocalDateTime createdAt;
        private List<ShareDetail> shares;
    }
}
