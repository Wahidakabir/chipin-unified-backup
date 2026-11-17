package com.chipin.chipin.repository;

import com.chipin.chipin.model.ItemizedBill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemizedBillRepository extends JpaRepository<ItemizedBill, Long> {
    List<ItemizedBill> findByGroupId(String groupId);
    List<ItemizedBill> findByGroupIdOrderByIdDesc(String groupId);
}
