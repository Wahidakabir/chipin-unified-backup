package com.chipin.chipin.repository;

import com.chipin.chipin.model.DebtSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DebtSettlementRepository extends JpaRepository<DebtSettlement, Long> {
    List<DebtSettlement> findByGroupIdOrderBySettledAtDesc(String groupId);
}
