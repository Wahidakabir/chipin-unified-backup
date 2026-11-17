package com.chipin.chipin.repository;

import com.chipin.chipin.model.BillShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillShareRepository extends JpaRepository<BillShare, Long> {
    List<BillShare> findByBillId(Long billId);
}