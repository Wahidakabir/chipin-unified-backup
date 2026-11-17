package com.chipin.chipin.repository;

import com.chipin.chipin.model.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA Repository for immutable Contribution ledger records.
 * Provides historical queries and aggregation calculations.
 */
@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    /**
     * Retrieves the chronological contribution history for a goal (newest contributions first).
     *
     * @param goalId the savings goal ID
     * @return list of contributions sorted descending by contribution timestamp
     */
    List<Contribution> findByGoalIdOrderByContributedAtDesc(Long goalId);

    /**
     * Retrieves all contributions made by a specific member toward a goal.
     *
     * @param goalId the savings goal ID
     * @param memberId the contributor member ID
     * @return list of matching contributions
     */
    List<Contribution> findByGoalIdAndMemberIdOrderByContributedAtDesc(Long goalId, String memberId);

    /**
     * Calculates the exact total amount saved toward a goal by summing ledger entries.
     *
     * @param goalId the savings goal ID
     * @return total sum of contributed funds, or 0.00 if no contributions exist
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.goal.id = :goalId")
    BigDecimal sumAmountByGoalId(@Param("goalId") Long goalId);

    /**
     * Calculates the total amount contributed by an individual member toward a goal.
     *
     * @param goalId the savings goal ID
     * @param memberId the member ID
     * @return total sum contributed by this member
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.goal.id = :goalId AND c.memberId = :memberId")
    BigDecimal sumAmountByGoalIdAndMemberId(@Param("goalId") Long goalId, @Param("memberId") String memberId);
}
