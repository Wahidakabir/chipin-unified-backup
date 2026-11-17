package com.chipin.chipin.controller;

import com.chipin.chipin.model.EmergencyExpense;
import com.chipin.chipin.service.EmergencyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @GetMapping
    public String viewEmergencyPage(@RequestParam(defaultValue = "Musarrat") String currentUser, Model model) {
        List<EmergencyExpense> emergencies = emergencyService.getGroupEmergencies(DEFAULT_GROUP_ID);
        model.addAttribute("emergencies", emergencies);
        model.addAttribute("contributionsMap", emergencyService.getContributionsMap(emergencies));
        model.addAttribute("newEmergency", new EmergencyExpense());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        return "emergency";
    }

    @PostMapping("/create")
    public String createEmergency(@Valid @ModelAttribute("newEmergency") EmergencyExpense newEmergency,
                                  BindingResult bindingResult,
                                  @RequestParam String currentUser,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            List<EmergencyExpense> emergencies = emergencyService.getGroupEmergencies(DEFAULT_GROUP_ID);
            model.addAttribute("emergencies", emergencies);
            model.addAttribute("contributionsMap", emergencyService.getContributionsMap(emergencies));
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("members", DEMO_MEMBERS);
            return "emergency";
        }

        emergencyService.createEmergency(newEmergency, DEFAULT_GROUP_ID, currentUser);
        redirectAttributes.addFlashAttribute("successMessage",
                "Emergency alert raised! Voting bypassed for instant member contributions.");
        return "redirect:/emergency?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/contribute")
    public String contributeToEmergency(@PathVariable Long id,
                                        @RequestParam String currentUser,
                                        @RequestParam double amount,
                                        RedirectAttributes redirectAttributes) {
        try {
            emergencyService.contribute(id, currentUser, amount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Instant contribution of ৳" + amount + " recorded from " + currentUser + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/emergency?currentUser=" + currentUser;
    }
}