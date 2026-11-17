package com.chipin.chipin.service;

import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.ExpenseReceipt;
import com.chipin.chipin.repository.ExpenseReceiptRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReceiptService {

    private final ExpenseReceiptRepository receiptRepository;
    private final ExpenseRepository expenseRepository;
    private final Path uploadDir = Paths.get("uploads", "receipts");

    public ReceiptService(ExpenseReceiptRepository receiptRepository, ExpenseRepository expenseRepository) {
        this.receiptRepository = receiptRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<ExpenseReceipt> getGroupReceipts(String groupId) {
        return receiptRepository.findByGroupIdOrderByUploadedAtDesc(groupId);
    }

    public List<Expense> getGroupExpenses(String groupId) {
        return expenseRepository.findByGroupId(groupId);
    }

    @Transactional
    public ExpenseReceipt uploadReceipt(MultipartFile file,
                                        String groupId,
                                        String uploaderId,
                                        Long linkedExpenseId,
                                        String merchantName,
                                        double receiptAmount,
                                        String itemSummary) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a receipt image or PDF file to upload.");
        }
        if (receiptAmount <= 0) {
            throw new IllegalArgumentException("Receipt amount must be greater than 0.");
        }

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt.png";
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }

        String storedName = UUID.randomUUID() + extension;
        Path targetPath = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        ExpenseReceipt receipt = new ExpenseReceipt();
        receipt.setGroupId(groupId);
        receipt.setLinkedExpenseId(linkedExpenseId);
        receipt.setUploadedByUserId(uploaderId);
        receipt.setMerchantName(merchantName);
        receipt.setReceiptAmount(Math.round(receiptAmount * 100.0) / 100.0);
        receipt.setItemSummary(itemSummary);
        receipt.setOriginalFileName(originalName);
        receipt.setStoredFileName(storedName);
        receipt.setFileType(".pdf".equals(extension) ? "PDF" : "IMAGE");
        receipt.setVerificationStatus("UNVERIFIED");
        receipt.setUploadedAt(LocalDateTime.now());

        return receiptRepository.save(receipt);
    }

    @Transactional
    public ExpenseReceipt auditReceipt(Long receiptId, String auditorId, String decision, String auditNote) {
        ExpenseReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found."));

        if (receipt.getUploadedByUserId().equalsIgnoreCase(auditorId)) {
            throw new IllegalStateException("Audit rule: You cannot verify or flag your own uploaded receipt. Switch to another member.");
        }

        if (!"VERIFIED".equals(decision) && !"FLAGGED".equals(decision)) {
            throw new IllegalArgumentException("Decision must be VERIFIED or FLAGGED.");
        }

        receipt.setVerificationStatus(decision);
        receipt.setVerifiedByUserId(auditorId);
        receipt.setAuditNote(auditNote != null && !auditNote.isBlank() ? auditNote : decision + " by " + auditorId);

        return receiptRepository.save(receipt);
    }
}