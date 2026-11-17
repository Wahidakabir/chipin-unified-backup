package com.chipin.chipin.controller;

import com.chipin.chipin.service.ItemizedBillService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// Feature 2: Itemized Receipt Splitting with Proportional Tax and Tip Allocation
@Controller
@RequestMapping("/bills")
public class ItemizedBillController {

    private final ItemizedBillService billService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public ItemizedBillController(ItemizedBillService billService) {
        this.billService = billService;
    }

    @GetMapping
    public String viewBillsPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        model.addAttribute("bills", billService.getGroupBillViews(DEFAULT_GROUP_ID));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        model.addAttribute("today", LocalDate.now());
        return "bills";
    }

    @PostMapping("/create")
    public String createBill(@RequestParam String currentUser,
                             @RequestParam String title,
                             @RequestParam String paidByUserId,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billDate,
                             @RequestParam(required = false) BigDecimal tax,
                             @RequestParam(required = false) BigDecimal serviceCharge,
                             @RequestParam(required = false) BigDecimal tip,
                             @RequestParam(required = false) BigDecimal discount,
                             RedirectAttributes redirectAttributes) {
        try {
            billService.createBill(DEFAULT_GROUP_ID, paidByUserId, title, billDate,
                    tax, serviceCharge, tip, discount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bill created! Now add the items and choose who shared each one.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bills?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id,
                          @RequestParam String currentUser,
                          @RequestParam String name,
                          @RequestParam BigDecimal amount,
                          @RequestParam(required = false) List<String> assignees,
                          RedirectAttributes redirectAttributes) {
        try {
            billService.addItem(id, name, amount, assignees);
            redirectAttributes.addFlashAttribute("successMessage", "Item added to the bill.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bills?currentUser=" + currentUser;
    }

    @PostMapping("/items/{itemId}/delete")
    public String removeItem(@PathVariable Long itemId,
                             @RequestParam String currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            billService.removeItem(itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bills?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/extras")
    public String updateExtras(@PathVariable Long id,
                               @RequestParam String currentUser,
                               @RequestParam(required = false) BigDecimal tax,
                               @RequestParam(required = false) BigDecimal serviceCharge,
                               @RequestParam(required = false) BigDecimal tip,
                               @RequestParam(required = false) BigDecimal discount,
                               RedirectAttributes redirectAttributes) {
        try {
            billService.updateExtras(id, tax, serviceCharge, tip, discount);
            redirectAttributes.addFlashAttribute("successMessage", "Tax, tip and discount updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bills?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/finalize")
    public String finalizeBill(@PathVariable Long id,
                               @RequestParam String currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            billService.finalizeBill(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bill finalized! Each member's share was saved and the expense was logged to the group ledger.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bills?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/delete")
    public String deleteDraft(@PathVariable Long id,
                              @RequestParam String currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            billService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Draft bill deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bills?currentUser=" + currentUser;
    }
}
