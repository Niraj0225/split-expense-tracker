package com.nirajmali.split_expense_tracker.dto;

import com.nirajmali.split_expense_tracker.entity.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class GroupDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateGroupRequest{

        @NotBlank(message = "Group name is required")
        private String name;

        @NotEmpty(message = "Add at least one member")
        private List<Long> membersIds;

    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemberSummary{
        private Long userId;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupResponse{
        private Long id;
        private String name;
        private String createdByName;
        private List<MemberSummary> members;
    }
//
//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class PageResponse<T>{
//        private List<T> content;
//        private int pageNumber;
//        private int pageSize;
//        private long totalElements;
//        private int totalPages;
//        private boolean isLastPage;
//    }
}
