package com.nirajmali.split_expense_tracker.service;

import com.nirajmali.split_expense_tracker.dto.ExpenseDTO;
import com.nirajmali.split_expense_tracker.dto.PageResponse;

public interface ExpenseService {


    public ExpenseDTO.ExpenseResponse createExpense(ExpenseDTO.CreateExpenseRequest request);

    public PageResponse<ExpenseDTO.ExpenseResponse> getExpensesForGroup(Long groupId, int page, int size);

}
