package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_receipts")
public class ExpenseReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private Long linkedExpenseId; // Optional link to an approved Expense in the ledger
    private String uploadedByUserId;

    private String merchantName;
    private double receiptAmount;
    private String itemSummary;

    private String originalFileName;
    private String storedFileName;
    private String fileType; // "IMAGE" or "PDF"

    private String verificationStatus; // "UNVERIFIED", "VERIFIED", "FLAGGED"
    private String verifiedByUserId;
    private String auditNote;

    private LocalDateTime uploadedAt;

    public ExpenseReceipt() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public Long getLinkedExpenseId() { return linkedExpenseId; }
    public void setLinkedExpenseId(Long linkedExpenseId) { this.linkedExpenseId = linkedExpenseId; }
    public String getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(String uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public double getReceiptAmount() { return receiptAmount; }
    public void setReceiptAmount(double receiptAmount) { this.receiptAmount = receiptAmount; }
    public String getItemSummary() { return itemSummary; }
    public void setItemSummary(String itemSummary) { this.itemSummary = itemSummary; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getVerifiedByUserId() { return verifiedByUserId; }
    public void setVerifiedByUserId(String verifiedByUserId) { this.verifiedByUserId = verifiedByUserId; }
    public String getAuditNote() { return auditNote; }
    public void setAuditNote(String auditNote) { this.auditNote = auditNote; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}