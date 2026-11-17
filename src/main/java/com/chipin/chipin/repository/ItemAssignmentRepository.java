package com.chipin.chipin.repository;

import com.chipin.chipin.model.ItemAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemAssignmentRepository extends JpaRepository<ItemAssignment, Long> {
    List<ItemAssignment> findByItemId(Long itemId);
    List<ItemAssignment> findByItemIdIn(List<Long> itemIds);
}