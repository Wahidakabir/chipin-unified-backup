package com.chipin.chipin.dto;

import com.chipin.chipin.model.ContributionPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing an individual member's planned contribution pledge.
 */
public class ContributionPlanResponseDTO {

    private Long id;
    private String memberId;
    private String memberName;
    private BigDecimal plannedAmount;
    private LocalDateTime updatedAt;

    public ContributionPlanResponseDTO() {
    }

    public ContributionPlanResponseDTO(Long id, String memberId, String memberName,
                                       BigDecimal plannedAmount, LocalDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.plannedAmount = plannedAmount;
        this.updatedAt = updatedAt;
    }

    /**
     * Maps a ContributionPlan JPA entity to its corresponding response DTO.
     *
     * @param plan the entity to map
     * @return response DTO, or null if entity is null
     */
    public static ContributionPlanResponseDTO fromEntity(ContributionPlan plan) {
        if (plan == null) {
            return null;
        }
        return new ContributionPlanResponseDTO(
                plan.getId(),
                plan.getMemberId(),
                plan.getMemberName(),
                plan.getPlannedAmount(),
                plan.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
