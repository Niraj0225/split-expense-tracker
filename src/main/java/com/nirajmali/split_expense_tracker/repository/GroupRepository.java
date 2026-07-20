package com.nirajmali.split_expense_tracker.repository;

import com.nirajmali.split_expense_tracker.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.id= :userId")
    Page<Group> findAllGroupsByMemberId(@Param("userId") Long userId, Pageable pageable);

}
