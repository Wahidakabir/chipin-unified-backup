package com.chipin.chipin.dto;

import com.chipin.chipin.model.Contribution;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing an immutable financial contribution ledger record.
 */
public class ContributionResponseDTO {

    private Long id;
    private String memberId;
    private String memberName;
    private BigDecimal amount;
    private LocalDateTime contributedAt;
    private String note;

    public ContributionResponseDTO() {
    }

    public ContributionResponseDTO(Long id, String memberId, String memberName,
                                   BigDecimal amount, LocalDateTime contributedAt, String note) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
        this.contributedAt = contributedAt;
        this.note = note;
    }

    /**
     * Maps a Contribution JPA entity to its corresponding response DTO.
     *
     * @param contribution the entity to map
     * @return response DTO, or null if entity is null
     */
    public static ContributionResponseDTO fromEntity(Contribution contribution) {
        if (contribution == null) {
            return null;
        }
        return new ContributionResponseDTO(
                contribution.getId(),
                contribution.getMemberId(),
                contribution.getMemberName(),
                contribution.getAmount(),
                contribution.getContributedAt(),
                contribution.getNote()
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getContributedAt() {
        return contributedAt;
    }

    public void setContributedAt(LocalDateTime contributedAt) {
        this.contributedAt = contributedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
