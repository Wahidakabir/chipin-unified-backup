package com.chipin.chipin.repository;

import com.chipin.chipin.model.GroupBudgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupBudgetConfigRepository extends JpaRepository<GroupBudgetConfig, Long> {
    Optional<GroupBudgetConfig> findByGroupId(String groupId);
}