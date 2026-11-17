package com.chipin.chipin.service;

import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.RecurringExpense;
import com.chipin.chipin.repository.ExpenseRepository;
import com.chipin.chipin.repository.RecurringExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringRepository;
    private final ExpenseRepository expenseRepository;

    public RecurringExpenseService(RecurringExpenseRepository recurringRepository,
                                   ExpenseRepository expenseRepository) {
        this.recurringRepository = recurringRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<RecurringExpense> getGroupRecurringExpenses(String groupId) {
        return recurringRepository.findByGroupIdOrderByNextDueDateAsc(groupId);
    }

    @Transactional
    public RecurringExpense createRecurringExpense(RecurringExpense recurring,
                                                   String groupId,
                                                   String creatorId,
                                                   List<String> members) {
        recurring.setGroupId(groupId);
        recurring.setCreatedByUserId(creatorId);
        recurring.setActive(true);

        if (recurring.getNextDueDate() == null) {
            recurring.setNextDueDate(LocalDate.now());
        }

        // Compute equal split across group members
        Map<String, Double> equalSplits = new LinkedHashMap<>();
        if (members != null && !members.isEmpty()) {
            double perMember = Math.round((recurring.getAmount() / members.size()) * 100.0) / 100.0;
            for (String member : members) {
                equalSplits.put(member, perMember);
            }
        }
        recurring.setSplits(equalSplits);

        return recurringRepository.save(recurring);
    }

    @Transactional
    public void toggleStatus(Long id) {
        RecurringExpense recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurring schedule not found."));
        recurring.setActive(!recurring.isActive());
        recurringRepository.save(recurring);
    }

    @Transactional
    public Expense triggerBillingCycle(Long id) {
        RecurringExpense recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurring schedule not found."));

        if (!recurring.isActive()) {
            throw new IllegalStateException("Cannot process a paused recurring schedule. Resume it first.");
        }

        Expense generated = new Expense();
        generated.setGroupId(recurring.getGroupId());
        generated.setPaidByUserId(recurring.getCreatedByUserId());
        generated.setTitle("[RECURRING] " + recurring.getTitle());
        generated.setCategory(recurring.getCategory() != null ? recurring.getCategory() : "Recurring Bill");
        generated.setTotalAmount(recurring.getAmount());
        generated.setSplits(new LinkedHashMap<>(recurring.getSplits()));
        generated.setStatus("APPROVED");
        generated.setCreatedAt(LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(generated);

        LocalDate today = LocalDate.now();
        recurring.setLastProcessedDate(today);
        LocalDate baseDate = recurring.getNextDueDate() != null ? recurring.getNextDueDate() : today;
        recurring.setNextDueDate(advanceDate(baseDate, recurring.getFrequency()));
        recurringRepository.save(recurring);

        return savedExpense;
    }

    private LocalDate advanceDate(LocalDate current, String frequency) {
        if ("WEEKLY".equalsIgnoreCase(frequency)) {
            return current.plusWeeks(1);
        } else if ("YEARLY".equalsIgnoreCase(frequency)) {
            return current.plusYears(1);
        }
        return current.plusMonths(1);
    }
}