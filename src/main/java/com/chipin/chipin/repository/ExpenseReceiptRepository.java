package com.chipin.chipin.repository;

import com.chipin.chipin.model.ExpenseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseReceiptRepository extends JpaRepository<ExpenseReceipt, Long> {
    List<ExpenseReceipt> findByGroupIdOrderByUploadedAtDesc(String groupId);
}