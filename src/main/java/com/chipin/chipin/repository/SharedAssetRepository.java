package com.chipin.chipin.repository;

import com.chipin.chipin.model.SharedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SharedAssetRepository extends JpaRepository<SharedAsset, Long> {
    List<SharedAsset> findByGroupIdOrderByIdDesc(String groupId);
}
