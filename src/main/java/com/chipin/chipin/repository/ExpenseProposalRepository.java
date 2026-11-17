package com.chipin.chipin.repository;

import com.chipin.chipin.model.ExpenseProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseProposalRepository extends JpaRepository<ExpenseProposal, Long> {
    List<ExpenseProposal> findByGroupIdOrderByCreatedAtDesc(String groupId);
}