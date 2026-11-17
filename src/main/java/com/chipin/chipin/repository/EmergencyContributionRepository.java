package com.chipin.chipin.repository;

import com.chipin.chipin.model.EmergencyContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyContributionRepository extends JpaRepository<EmergencyContribution, Long> {
    List<EmergencyContribution> findByEmergencyExpenseIdOrderByContributedAtDesc(Long emergencyExpenseId);
}