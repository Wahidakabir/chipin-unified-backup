package com.chipin.chipin.repository;

import com.chipin.chipin.model.RoundUpSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoundUpSettingsRepository extends JpaRepository<RoundUpSettings, Long> {
    Optional<RoundUpSettings> findByGroupIdAndUserId(String groupId, String userId);
}
