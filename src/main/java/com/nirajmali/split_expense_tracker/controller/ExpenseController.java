package com.nirajmali.split_expense_tracker.controller;

import com.nirajmali.split_expense_tracker.dto.ExpenseDTO;
import com.nirajmali.split_expense_tracker.dto.PageResponse;
import com.nirajmali.split_expense_tracker.entity.Group;
import com.nirajmali.split_expense_tracker.entity.User;
import com.nirajmali.split_expense_tracker.repository.GroupRepository;
import com.nirajmali.split_expense_tracker.repository.UserRepository;
import com.nirajmali.split_expense_tracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @PostMapping
    public ResponseEntity<ExpenseDTO.ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseDTO.CreateExpenseRequest request,
             @AuthenticationPrincipal UserDetails userDetails
    ){
        User loggedInUser=userRepository.findByEmail(userDetails.getUsername()).orElseThrow(()->new RuntimeException("User not found"));

        Group group=groupRepository.findById(request.getGroupId()).orElseThrow(()->new RuntimeException("Group not found"));

        boolean isMember=group.getMembers()
                .stream()
                .anyMatch(member->member.getId().equals(loggedInUser.getId()));

        if (!isMember){
            throw new RuntimeException("You are not a member of this group");
        }
        ExpenseDTO.ExpenseResponse response=expenseService.createExpense(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping("/group/{groupId}")
    public ResponseEntity<PageResponse<ExpenseDTO.ExpenseResponse>> getExpenseForGroup(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size)
    {
        PageResponse<ExpenseDTO.ExpenseResponse> response=expenseService.getExpensesForGroup(groupId, page, size);

        return ResponseEntity.ok(response);
    }


}
