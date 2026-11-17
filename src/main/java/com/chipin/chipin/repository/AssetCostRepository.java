package com.chipin.chipin.repository;

import com.chipin.chipin.model.AssetCost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetCostRepository extends JpaRepository<AssetCost, Long> {
    List<AssetCost> findByAssetIdOrderByIncurredAtDesc(Long assetId);
}
