package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bill_shares")
public class BillShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long billId;
    private String userId;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal extrasShare = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    public BillShare() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getExtrasShare() { return extrasShare; }
    public void setExtrasShare(BigDecimal extrasShare) { this.extrasShare = extrasShare; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}