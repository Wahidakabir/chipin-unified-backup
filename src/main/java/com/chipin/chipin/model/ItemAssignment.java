package com.chipin.chipin.model;

import jakarta.persistence.*;

@Entity
@Table(name = "item_assignments")
public class ItemAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long itemId;
    private String userId;

    public ItemAssignment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}