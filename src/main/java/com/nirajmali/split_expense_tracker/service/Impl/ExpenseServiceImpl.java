package com.nirajmali.split_expense_tracker.service.Impl;

import com.nirajmali.split_expense_tracker.dto.ExpenseDTO;
import com.nirajmali.split_expense_tracker.dto.ExpenseEvent;
import com.nirajmali.split_expense_tracker.dto.PageResponse;
import com.nirajmali.split_expense_tracker.entity.*;
import com.nirajmali.split_expense_tracker.kafka.ExpenseEventProducer;
import com.nirajmali.split_expense_tracker.repository.ExpenseRepository;
import com.nirajmali.split_expense_tracker.repository.GroupRepository;
import com.nirajmali.split_expense_tracker.repository.UserRepository;
import com.nirajmali.split_expense_tracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ExpenseEventProducer expenseEventProducer;

    @Transactional
    @Override
    public ExpenseDTO.ExpenseResponse createExpense(ExpenseDTO.CreateExpenseRequest request) {
        System.out.println("=== DEBUG ===");
        System.out.println("splitType: " + request.getSplitType());
        System.out.println("customShares: " + request.getCustomShares());
        System.out.println("=============");

        Group group=groupRepository.findById(request.getGroupId())
                .orElseThrow(()->new RuntimeException("Group not found: "+ request.getGroupId()));

        User paidBy=userRepository.findById(request.getPaidByUserId())
                .orElseThrow(()->new RuntimeException("User not found: "+ request.getPaidByUserId()));

        Expense expense=Expense.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .group(group)
                .paidBy(paidBy)
                .splitType(request.getSplitType())
                .build();

        List<ExpenseShare> share= calculateShares(expense, group, request);

        expense.setShares(share);

        Expense savedExpense=expenseRepository.save(expense);
        ExpenseEvent event=ExpenseEvent.builder()
                .eventType("EXPENSE_CREATED")
                .groupId(group.getId())
                .triggeredByUserId(paidBy.getId())
                .occurredAt(LocalDateTime.now())
                .build();
        expenseEventProducer.publishExpenseEvent(event);
        return toResponse(savedExpense);
    }

    @Override
    public PageResponse<ExpenseDTO.ExpenseResponse> getExpensesForGroup(Long groupId, int page, int size) {
        groupRepository.findById(groupId).orElseThrow(()->new RuntimeException("Group not found: " + groupId));

        Pageable pageable= PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt"));

        Page<Expense> expensePage=expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        List<ExpenseDTO.ExpenseResponse> content=expensePage
                .getContent()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ExpenseDTO.ExpenseResponse>builder()
                .content(content)
                .pageNumber(expensePage.getNumber())
                .pageSize(expensePage.getSize())
                .totalElements(expensePage.getTotalElements())
                .totalPages(expensePage.getTotalPages())
                .isLastPage(expensePage.isLast())
                .build();
    }


    private List<ExpenseShare> calculateShares(Expense expense, Group group, ExpenseDTO.CreateExpenseRequest request) {
        List<User> members=new ArrayList<>(group.getMembers());
        List<ExpenseShare> shares= new ArrayList<>();

        if (request.getSplitType() == SplitType.EQUAL){
            int memberCount=members.size();
            BigDecimal equalShare=request.getAmount()
                    .divide(BigDecimal.valueOf(memberCount),2, RoundingMode.HALF_UP);

            BigDecimal totalAssigned=BigDecimal.ZERO;

            for (int i = 0; i < members.size(); i++) {
                BigDecimal shareAmount;

                if (i==members.size()-1){
                    shareAmount=request.getAmount()
                            .subtract(totalAssigned);
                }else {
                    shareAmount=equalShare;
                    totalAssigned=totalAssigned.add(shareAmount);
                }
                shares.add(ExpenseShare.builder()
                        .expense(expense)
                        .user(members.get(i))
                        .shareAmount(shareAmount)
                        .build());
            }

        }
        else if(request.getSplitType()== SplitType.EXACT) {
            Map<String, BigDecimal> customerShares=request.getCustomShares();

            if (customerShares ==null|| customerShares.isEmpty()){
                throw new RuntimeException("Custom shares required for EXACT split");
            }

            BigDecimal total=customerShares.values()
                    .stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (total.compareTo(request.getAmount()) != 0){
                throw new RuntimeException("Shares must sum to " + request.getAmount() + " but got " + total);
            }

            for (Map.Entry<String, BigDecimal> entry: customerShares.entrySet()){
                Long userId= Long.parseLong(entry.getKey());
                User user=userRepository.findById(userId)
                        .orElseThrow(()->new RuntimeException("User not found: " + userId));

                shares.add(ExpenseShare.builder()
                        .expense(expense)
                        .user(user)
                        .shareAmount(entry.getValue())
                        .build());
            }
        } else if (request.getSplitType() == SplitType.PERCENTAGE) {
            Map<String, BigDecimal> customShares =
                    request.getCustomShares();

            if (customShares == null || customShares.isEmpty()) {
                throw new RuntimeException(
                        "Custom shares required for PERCENTAGE split");
            }

            BigDecimal totalPercentage=customShares.values()
                    .stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0){
                throw new RuntimeException("Percentages must sum to 100 but got "+ totalPercentage);
            }
            for (Map.Entry<String, BigDecimal> entry : customShares.entrySet()){
                Long userId= Long.parseLong(entry.getKey());
                User user=userRepository.findById(userId)
                        .orElseThrow(()->new RuntimeException("User not found: " + userId));

                BigDecimal shareAmount= request.getAmount()
                        .multiply(entry.getValue())
                        .divide(BigDecimal.valueOf(100),
                                2, RoundingMode.HALF_UP);
                shares.add(ExpenseShare.builder()
                        .expense(expense)
                        .user(user)
                        .shareAmount(shareAmount)
                        .build());
            }

        }
        return shares;

    }

    private ExpenseDTO.ExpenseResponse toResponse(Expense savedExpense) {
        List<ExpenseDTO.ShareDetail> details=savedExpense.getShares()
                .stream()
                .map(share-> ExpenseDTO.ShareDetail.builder()
                        .userId(share.getUser().getId())
                        .userName(share.getUser().getName())
                        .shareAmount(share.getShareAmount())
                        .build())
                .collect(Collectors.toList());

        return ExpenseDTO.ExpenseResponse.builder()
                .id(savedExpense.getId())
                .description(savedExpense.getDescription())
                .amount(savedExpense.getAmount())
                .paidByName(savedExpense.getPaidBy().getName())
                .splitType(savedExpense.getSplitType())
                .createdAt(savedExpense.getCreatedAt())
                .shares(details)
                .build();
    }
}
