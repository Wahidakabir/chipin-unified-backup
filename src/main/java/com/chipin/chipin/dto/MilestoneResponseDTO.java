package com.chipin.chipin.dto;

import com.chipin.chipin.model.Milestone;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing a savings goal milestone checkpoint (25%, 50%, 75%, 100%).
 */
public class MilestoneResponseDTO {

    private Integer percentage;
    private BigDecimal targetAmountSnapshot;
    private boolean isAchieved;
    private LocalDateTime achievedAt;

    public MilestoneResponseDTO() {
    }

    public MilestoneResponseDTO(Integer percentage, BigDecimal targetAmountSnapshot,
                                boolean isAchieved, LocalDateTime achievedAt) {
        this.percentage = percentage;
        this.targetAmountSnapshot = targetAmountSnapshot;
        this.isAchieved = isAchieved;
        this.achievedAt = achievedAt;
    }

    /**
     * Maps a Milestone JPA entity to its corresponding response DTO.
     *
     * @param milestone the entity to map
     * @return response DTO, or null if entity is null
     */
    public static MilestoneResponseDTO fromEntity(Milestone milestone) {
        if (milestone == null) {
            return null;
        }
        return new MilestoneResponseDTO(
                milestone.getPercentage(),
                milestone.getTargetAmountSnapshot(),
                milestone.isAchieved(),
                milestone.getAchievedAt()
        );
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public BigDecimal getTargetAmountSnapshot() {
        return targetAmountSnapshot;
    }

    public void setTargetAmountSnapshot(BigDecimal targetAmountSnapshot) {
        this.targetAmountSnapshot = targetAmountSnapshot;
    }

    public boolean isAchieved() {
        return isAchieved;
    }

    public void setAchieved(boolean achieved) {
        isAchieved = achieved;
    }

    public LocalDateTime getAchievedAt() {
        return achievedAt;
    }

    public void setAchievedAt(LocalDateTime achievedAt) {
        this.achievedAt = achievedAt;
    }
}
