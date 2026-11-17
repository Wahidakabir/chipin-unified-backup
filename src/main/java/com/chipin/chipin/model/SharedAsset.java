package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Something lasting the group bought together, e.g. a washing machine (Feature 5).
@Entity
@Table(name = "shared_assets")
public class SharedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String name;
    @Column(precision = 12, scale = 2)
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private int usefulLifeMonths;
    private String createdByUserId;
    private LocalDateTime createdAt;

    public SharedAsset() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public int getUsefulLifeMonths() { return usefulLifeMonths; }
    public void setUsefulLifeMonths(int usefulLifeMonths) { this.usefulLifeMonths = usefulLifeMonths; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
