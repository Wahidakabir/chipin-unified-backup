package com.chipin.chipin.repository;

import com.chipin.chipin.model.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {
    List<BillItem> findByBillId(Long billId);
}