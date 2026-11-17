package com.chipin.chipin.repository;

import com.chipin.chipin.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupId(String groupId);
}