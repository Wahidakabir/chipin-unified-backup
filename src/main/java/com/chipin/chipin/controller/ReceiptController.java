package com.chipin.chipin.controller;

import com.chipin.chipin.service.ReceiptService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping
    public String viewReceiptsPage(@RequestParam(defaultValue = "Musarrat") String currentUser, Model model) {
        model.addAttribute("receipts", receiptService.getGroupReceipts(DEFAULT_GROUP_ID));
        model.addAttribute("expenses", receiptService.getGroupExpenses(DEFAULT_GROUP_ID));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        return "receipts";
    }

    @PostMapping("/upload")
    public String uploadReceipt(@RequestParam("file") MultipartFile file,
                                @RequestParam String currentUser,
                                @RequestParam(required = false) Long linkedExpenseId,
                                @RequestParam String merchantName,
                                @RequestParam double receiptAmount,
                                @RequestParam(required = false) String itemSummary,
                                RedirectAttributes redirectAttributes) {
        try {
            receiptService.uploadReceipt(file, DEFAULT_GROUP_ID, currentUser,
                    linkedExpenseId, merchantName, receiptAmount, itemSummary);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Receipt uploaded successfully! Awaiting peer audit verification.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/receipts?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/audit")
    public String auditReceipt(@PathVariable Long id,
                               @RequestParam String currentUser,
                               @RequestParam String decision,
                               @RequestParam(required = false) String auditNote,
                               RedirectAttributes redirectAttributes) {
        try {
            receiptService.auditReceipt(id, currentUser, decision, auditNote);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Receipt marked as " + decision + " by " + currentUser + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/receipts?currentUser=" + currentUser;
    }
}