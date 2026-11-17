package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "expense_proposals")
public class ExpenseProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String proposedByUserId;

    @NotBlank(message = "Expense title is required")
    private String title;

    private String description;

    @Positive(message = "Amount must be greater than 0")
    private double amount;

    @NotBlank(message = "Approval rule is required")
    private String approvalRule; // "MAJORITY" or "UNANIMOUS"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "proposal_votes", joinColumns = @JoinColumn(name = "proposal_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "vote_choice")
    private Map<String, String> votes = new HashMap<>();

    private String status; // "PENDING", "APPROVED", "REJECTED"
    private LocalDateTime createdAt;

    public ExpenseProposal() {}

    public long getYesCount() {
        return votes.values().stream().filter("YES"::equals).count();
    }

    public long getNoCount() {
        return votes.values().stream().filter("NO"::equals).count();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getProposedByUserId() { return proposedByUserId; }
    public void setProposedByUserId(String proposedByUserId) { this.proposedByUserId = proposedByUserId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getApprovalRule() { return approvalRule; }
    public void setApprovalRule(String approvalRule) { this.approvalRule = approvalRule; }
    public Map<String, String> getVotes() { return votes; }
    public void setVotes(Map<String, String> votes) { this.votes = votes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}