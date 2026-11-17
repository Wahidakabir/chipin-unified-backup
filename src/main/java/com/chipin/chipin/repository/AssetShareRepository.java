package com.chipin.chipin.repository;

import com.chipin.chipin.model.AssetShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetShareRepository extends JpaRepository<AssetShare, Long> {
    List<AssetShare> findByAssetIdOrderByUserIdAsc(Long assetId);
}
