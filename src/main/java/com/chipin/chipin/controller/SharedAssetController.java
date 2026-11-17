package com.chipin.chipin.controller;

import com.chipin.chipin.service.SharedAssetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Feature 5: Shared Asset Co-Ownership with Usage-Based Costs and Buy-In/Buyout
@Controller
@RequestMapping("/assets")
public class SharedAssetController {

    private final SharedAssetService assetService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public SharedAssetController(SharedAssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public String viewAssetsPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        model.addAttribute("assets", assetService.getGroupAssetViews(DEFAULT_GROUP_ID, DEMO_MEMBERS));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        model.addAttribute("today", LocalDate.now());
        return "assets";
    }

    // The form sends one "paid" amount per member, in the same order as DEMO_MEMBERS.
    @PostMapping("/create")
    public String registerAsset(@RequestParam String currentUser,
                                @RequestParam String name,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                                @RequestParam int usefulLifeMonths,
                                @RequestParam List<String> paid,
                                RedirectAttributes redirectAttributes) {
        try {
            Map<String, BigDecimal> contributions = new LinkedHashMap<>();
            for (int i = 0; i < DEMO_MEMBERS.size() && i < paid.size(); i++) {
                String text = paid.get(i) == null ? "" : paid.get(i).trim();
                contributions.put(DEMO_MEMBERS.get(i), text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text));
            }
            assetService.registerAsset(DEFAULT_GROUP_ID, name, purchaseDate, usefulLifeMonths,
                    contributions, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Asset registered. Ownership shares follow what each member paid.");
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enter amounts as numbers, e.g. 6000.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/use")
    public String logUse(@PathVariable Long id, @RequestParam String currentUser,
                         RedirectAttributes redirectAttributes) {
        try {
            assetService.logUse(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Use logged for " + currentUser + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/costs")
    public String addCost(@PathVariable Long id,
                          @RequestParam String currentUser,
                          @RequestParam String description,
                          @RequestParam BigDecimal amount,
                          @RequestParam String paidByUserId,
                          RedirectAttributes redirectAttributes) {
        try {
            assetService.addCost(id, description, amount, paidByUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cost added and split. What each member owes is now on the Debts page.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/buyout")
    public String buyOut(@PathVariable Long id,
                         @RequestParam String currentUser,
                         @RequestParam String leavingUserId,
                         RedirectAttributes redirectAttributes) {
        try {
            BigDecimal buyout = assetService.buyOut(id, leavingUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    leavingUserId + " was bought out for ৳" + buyout + ". The remaining owners now owe them that on the Debts page.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/buyin")
    public String buyIn(@PathVariable Long id,
                        @RequestParam String currentUser,
                        @RequestParam String joiningUserId,
                        @RequestParam BigDecimal sharePercent,
                        RedirectAttributes redirectAttributes) {
        try {
            BigDecimal price = assetService.buyIn(id, joiningUserId, sharePercent);
            redirectAttributes.addFlashAttribute("successMessage",
                    joiningUserId + " joined for ৳" + price + ". They now owe the existing owners that on the Debts page.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }
}
