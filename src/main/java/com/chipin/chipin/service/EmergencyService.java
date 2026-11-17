package com.chipin.chipin.service;

import com.chipin.chipin.model.EmergencyContribution;
import com.chipin.chipin.model.EmergencyExpense;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.repository.EmergencyContributionRepository;
import com.chipin.chipin.repository.EmergencyExpenseRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmergencyService {

    private final EmergencyExpenseRepository emergencyRepository;
    private final EmergencyContributionRepository contributionRepository;
    private final ExpenseRepository expenseRepository;

    public EmergencyService(EmergencyExpenseRepository emergencyRepository,
                            EmergencyContributionRepository contributionRepository,
                            ExpenseRepository expenseRepository) {
        this.emergencyRepository = emergencyRepository;
        this.contributionRepository = contributionRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<EmergencyExpense> getGroupEmergencies(String groupId) {
        return emergencyRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    public Map<Long, List<EmergencyContribution>> getContributionsMap(List<EmergencyExpense> emergencies) {
        Map<Long, List<EmergencyContribution>> map = new LinkedHashMap<>();
        for (EmergencyExpense e : emergencies) {
            map.put(e.getId(), contributionRepository.findByEmergencyExpenseIdOrderByContributedAtDesc(e.getId()));
        }
        return map;
    }

    @Transactional
    public EmergencyExpense createEmergency(EmergencyExpense emergency, String groupId, String requesterId) {
        emergency.setGroupId(groupId);
        emergency.setRequestedByUserId(requesterId);
        emergency.setCollectedAmount(0.0);
        emergency.setStatus("ACTIVE");
        emergency.setCreatedAt(LocalDateTime.now());
        return emergencyRepository.save(emergency);
    }

    @Transactional
    public EmergencyExpense contribute(Long emergencyId, String contributorId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Contribution amount must be greater than 0.");
        }

        EmergencyExpense emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency request not found."));

        if ("FULFILLED".equals(emergency.getStatus())) {
            throw new IllegalStateException("This emergency fund has already reached its target.");
        }

        EmergencyContribution contribution = new EmergencyContribution();
        contribution.setEmergencyExpenseId(emergencyId);
        contribution.setContributorUserId(contributorId);
        contribution.setAmount(Math.round(amount * 100.0) / 100.0);
        contribution.setContributedAt(LocalDateTime.now());
        contributionRepository.save(contribution);

        double newCollected = Math.round((emergency.getCollectedAmount() + amount) * 100.0) / 100.0;
        emergency.setCollectedAmount(newCollected);

        if (newCollected >= emergency.getTargetAmount()) {
            emergency.setStatus("FULFILLED");
            recordFulfilledEmergencyExpense(emergency);
        }

        return emergencyRepository.save(emergency);
    }

    private void recordFulfilledEmergencyExpense(EmergencyExpense emergency) {
        List<EmergencyContribution> contributions =
                contributionRepository.findByEmergencyExpenseIdOrderByContributedAtDesc(emergency.getId());

        Map<String, Double> actualSplits = new HashMap<>();
        for (EmergencyContribution c : contributions) {
            actualSplits.merge(c.getContributorUserId(), c.getAmount(), Double::sum);
        }

        Expense settled = new Expense();
        settled.setGroupId(emergency.getGroupId());
        settled.setPaidByUserId(emergency.getRequestedByUserId());
        settled.setTitle("[EMERGENCY] " + emergency.getTitle());
        settled.setCategory("Emergency Expense");
        settled.setTotalAmount(emergency.getCollectedAmount());
        settled.setSplits(actualSplits);
        settled.setStatus("APPROVED");
        settled.setCreatedAt(LocalDateTime.now());

        expenseRepository.save(settled);
    }
}