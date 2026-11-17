package com.chipin.chipin.repository;

import com.chipin.chipin.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    List<RecurringExpense> findByGroupIdOrderByNextDueDateAsc(String groupId);
    List<RecurringExpense> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}