package com.nirajmali.split_expense_tracker.repository;

import com.nirajmali.split_expense_tracker.entity.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    @Query("SELECT es FROM ExpenseShare es " + "WHERE es.expense.group.id = :groupId")
    List<ExpenseShare> findAllByGroupId(@Param("groupId") Long groupId);
}
