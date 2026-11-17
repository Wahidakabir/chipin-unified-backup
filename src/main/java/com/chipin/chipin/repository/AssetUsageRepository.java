package com.chipin.chipin.repository;

import com.chipin.chipin.model.AssetUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetUsageRepository extends JpaRepository<AssetUsage, Long> {
    List<AssetUsage> findByAssetId(Long assetId);
}
