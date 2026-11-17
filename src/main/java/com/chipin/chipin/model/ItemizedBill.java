package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "itemized_bills")
public class ItemizedBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String paidByUserId;
    private String title;
    private LocalDate billDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal tip = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    private String status = "DRAFT"; // DRAFT or FINALIZED

    public ItemizedBill() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getPaidByUserId() { return paidByUserId; }
    public void setPaidByUserId(String paidByUserId) { this.paidByUserId = paidByUserId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getServiceCharge() { return serviceCharge; }
    public void setServiceCharge(BigDecimal serviceCharge) { this.serviceCharge = serviceCharge; }
    public BigDecimal getTip() { return tip; }
    public void setTip(BigDecimal tip) { this.tip = tip; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}