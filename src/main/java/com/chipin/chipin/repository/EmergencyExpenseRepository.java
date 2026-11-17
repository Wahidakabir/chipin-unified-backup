package com.chipin.chipin.repository;

import com.chipin.chipin.model.EmergencyExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyExpenseRepository extends JpaRepository<EmergencyExpense, Long> {
    List<EmergencyExpense> findByGroupIdOrderByCreatedAtDesc(String groupId);
}