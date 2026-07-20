package com.nirajmali.split_expense_tracker.repository;

import com.nirajmali.split_expense_tracker.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroupId(Long groupId);
}
