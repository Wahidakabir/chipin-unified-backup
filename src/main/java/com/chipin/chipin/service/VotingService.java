package com.chipin.chipin.service;

import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.ExpenseProposal;
import com.chipin.chipin.repository.ExpenseProposalRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VotingService {

    private final ExpenseProposalRepository proposalRepository;
    private final ExpenseRepository expenseRepository;

    private static final double SPENDING_APPROVAL_LIMIT = 5000.0;

    public VotingService(ExpenseProposalRepository proposalRepository,
                         ExpenseRepository expenseRepository) {
        this.proposalRepository = proposalRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<ExpenseProposal> getGroupProposals(String groupId) {
        return proposalRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    @Transactional
    public ExpenseProposal createProposal(ExpenseProposal proposal, String groupId,
                                          String creatorUserId, List<String> groupMemberIds) {
        if (!groupMemberIds.contains(creatorUserId)) {
            throw new IllegalStateException("Only group members can create proposals.");
        }

        proposal.setGroupId(groupId);
        proposal.setProposedByUserId(creatorUserId);
        proposal.setStatus("PENDING");
        proposal.setCreatedAt(LocalDateTime.now());

        Map<String, String> initialVotes = new HashMap<>();
        initialVotes.put(creatorUserId, "YES");
        proposal.setVotes(initialVotes);

        // Small expenses skip the vote and go straight to the ledger
        if (proposal.getAmount() <= SPENDING_APPROVAL_LIMIT) {
            approveAndRecordExpense(proposal, groupMemberIds);
        } else {
            evaluate(proposal, groupMemberIds); // handles tiny groups where the creator's YES already decides it
        }

        return proposalRepository.save(proposal);
    }

    @Transactional
    public ExpenseProposal castVote(Long proposalId, String userId, String voteChoice,
                                    List<String> groupMemberIds) {
        ExpenseProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found."));

        if (!"PENDING".equals(proposal.getStatus())) {
            throw new IllegalStateException("Voting is already closed for this proposal.");
        }
        if (!groupMemberIds.contains(userId)) {
            throw new IllegalStateException("Only group members can vote.");
        }
        if (!"YES".equals(voteChoice) && !"NO".equals(voteChoice)) {
            throw new IllegalArgumentException("Invalid vote choice. Must be YES or NO.");
        }

        // Members can change their vote while the proposal is still pending
        proposal.getVotes().put(userId, voteChoice);
        evaluate(proposal, groupMemberIds);

        return proposalRepository.save(proposal);
    }

    private void evaluate(ExpenseProposal proposal, List<String> groupMemberIds) {
        int total = groupMemberIds.size();
        long yes = proposal.getYesCount();
        long no = proposal.getNoCount();

        if ("UNANIMOUS".equalsIgnoreCase(proposal.getApprovalRule())) {
            if (no > 0) {
                proposal.setStatus("REJECTED");
            } else if (yes == total) {
                approveAndRecordExpense(proposal, groupMemberIds);
            }
        } else {
            int needed = (total / 2) + 1;
            if (yes >= needed) {
                approveAndRecordExpense(proposal, groupMemberIds);
            } else if (no > (total - needed)) {
                proposal.setStatus("REJECTED");
            }
        }
    }

    private void approveAndRecordExpense(ExpenseProposal proposal, List<String> groupMemberIds) {
        proposal.setStatus("APPROVED");

        Expense expense = new Expense();
        expense.setGroupId(proposal.getGroupId());
        expense.setPaidByUserId(proposal.getProposedByUserId());
        expense.setTitle(proposal.getTitle());
        expense.setCategory("Approved Expense");
        expense.setTotalAmount(proposal.getAmount());
        expense.setStatus("APPROVED");
        expense.setCreatedAt(LocalDateTime.now());

        // Equal split in cents; the last member absorbs the rounding difference
        Map<String, Double> splits = new LinkedHashMap<>();
        int n = groupMemberIds.size();
        double base = Math.floor(proposal.getAmount() / n * 100) / 100.0;
        double allocated = 0;
        for (int i = 0; i < n; i++) {
            double share = (i == n - 1)
                    ? Math.round((proposal.getAmount() - allocated) * 100) / 100.0
                    : base;
            splits.put(groupMemberIds.get(i), share);
            allocated += share;
        }
        expense.setSplits(splits);

        expenseRepository.save(expense);
    }

    public double getSpendingApprovalLimit() {
        return SPENDING_APPROVAL_LIMIT;
    }
}