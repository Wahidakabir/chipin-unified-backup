package com.chipin.chipin.controller;

import com.chipin.chipin.service.RoundUpService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

// Feature 3: Round-Up with Boosters
@Controller
@RequestMapping("/roundup")
public class RoundUpController {

    private final RoundUpService roundUpService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public RoundUpController(RoundUpService roundUpService) {
        this.roundUpService = roundUpService;
    }

    @GetMapping
    public String viewRoundUpPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        model.addAttribute("overview", roundUpService.getOverview(DEFAULT_GROUP_ID, currentUser));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        return "roundup";
    }

    // Unticked checkboxes are not sent by the browser, so they default to false.
    @PostMapping("/settings")
    public String saveSettings(@RequestParam String currentUser,
                               @RequestParam(defaultValue = "false") boolean enabled,
                               @RequestParam BigDecimal roundTo,
                               @RequestParam(defaultValue = "false") boolean weekendDoubleEnabled,
                               @RequestParam(defaultValue = "false") boolean fixedExtraEnabled,
                               @RequestParam(required = false) BigDecimal fixedExtraAmount,
                               @RequestParam String destinationType,
                               @RequestParam String destinationName,
                               RedirectAttributes redirectAttributes) {
        try {
            roundUpService.saveSettings(DEFAULT_GROUP_ID, currentUser, enabled, roundTo,
                    weekendDoubleEnabled, fixedExtraEnabled, fixedExtraAmount,
                    destinationType, destinationName);
            redirectAttributes.addFlashAttribute("successMessage", "Round-up rules saved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/roundup?currentUser=" + currentUser;
    }
}
