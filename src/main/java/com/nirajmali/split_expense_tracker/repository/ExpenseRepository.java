package com.nirajmali.split_expense_tracker.repository;

import com.nirajmali.split_expense_tracker.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
}
