package com.chipin.chipin.repository;

import com.chipin.chipin.model.RoundUpSaving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoundUpSavingRepository extends JpaRepository<RoundUpSaving, Long> {
    List<RoundUpSaving> findByGroupIdOrderByCreatedAtDesc(String groupId);
    List<RoundUpSaving> findBySettlementId(Long settlementId);
}
