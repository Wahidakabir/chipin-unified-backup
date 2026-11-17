package com.chipin.chipin;

import com.chipin.chipin.dto.ContributionResponseDTO;
import com.chipin.chipin.exception.OverfundingException;
import com.chipin.chipin.model.Contribution;
import com.chipin.chipin.model.GoalStatus;
import com.chipin.chipin.model.Milestone;
import com.chipin.chipin.model.SavingsGoal;
import com.chipin.chipin.repository.ContributionRepository;
import com.chipin.chipin.repository.MilestoneRepository;
import com.chipin.chipin.repository.SavingsGoalRepository;
import com.chipin.chipin.service.ContributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Feature 15: ContributionService Ledger & Overfunding Invariants Tests")
class ContributionServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @InjectMocks
    private ContributionService contributionService;

    private SavingsGoal sampleGoal;
    private List<Milestone> milestones;

    @BeforeEach
    void setUp() {
        sampleGoal = new SavingsGoal(
                "group_01",
                "Mufliha",
                "Laptop Fund",
                "New developer laptop",
                new BigDecimal("10000.00"),
                LocalDate.now().plusMonths(2)
        );
        sampleGoal.setId(1L);
        sampleGoal.setCurrentAmount(BigDecimal.ZERO);
        sampleGoal.setStatus(GoalStatus.ACTIVE);

        milestones = new ArrayList<>();
        milestones.add(new Milestone(sampleGoal, 25, new BigDecimal("2500.00")));
        milestones.add(new Milestone(sampleGoal, 50, new BigDecimal("5000.00")));
        milestones.add(new Milestone(sampleGoal, 75, new BigDecimal("7500.00")));
        milestones.add(new Milestone(sampleGoal, 100, new BigDecimal("10000.00")));
    }

    @Test
    @DisplayName("Should record contribution, increment balance, and unlock 25% milestone")
    void testRecordContribution_UnlocksMilestone() {
        when(savingsGoalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleGoal));
        when(milestoneRepository.findByGoalIdAndIsAchievedFalseOrderByPercentageAsc(1L)).thenReturn(milestones);
        when(contributionRepository.save(any(Contribution.class))).thenAnswer(inv -> {
            Contribution c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        // Contribute 3,000 (unlocks 25% milestone at 2,500)
        Contribution response = contributionService.recordContribution(
                1L, "Afifa", "Afifa", new BigDecimal("3000.00"), "First installment"
        );

        assertNotNull(response);
        assertEquals(new BigDecimal("3000.00"), response.getAmount());
        assertEquals("Afifa", response.getMemberName());
        assertEquals(new BigDecimal("3000.00"), sampleGoal.getCurrentAmount());
        assertEquals(GoalStatus.ACTIVE, sampleGoal.getStatus());

        // 25% milestone must be unlocked
        assertTrue(milestones.get(0).isAchieved());
        assertNotNull(milestones.get(0).getAchievedAt());

        // 50% milestone still pending
        assertFalse(milestones.get(1).isAchieved());

        verify(savingsGoalRepository).save(sampleGoal);
        verify(contributionRepository).save(any(Contribution.class));
    }

    @Test
    @DisplayName("Should complete goal when contributions reach 100% target")
    void testRecordContribution_CompletesGoal() {
        sampleGoal.setCurrentAmount(new BigDecimal("8000.00"));
        milestones.get(0).setAchieved(true);
        milestones.get(1).setAchieved(true);
        milestones.get(2).setAchieved(true);
        List<Milestone> remaining = List.of(milestones.get(3));

        when(savingsGoalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleGoal));
        when(milestoneRepository.findByGoalIdAndIsAchievedFalseOrderByPercentageAsc(1L)).thenReturn(remaining);
        when(contributionRepository.save(any(Contribution.class))).thenAnswer(inv -> inv.getArgument(0));

        // Remaining is 2,000 -> contributing 2,000 completes goal
        contributionService.recordContribution(
                1L, "Sayma", "Sayma", new BigDecimal("2000.00"), "Final push"
        );

        assertEquals(new BigDecimal("10000.00"), sampleGoal.getCurrentAmount());
        assertEquals(GoalStatus.COMPLETED, sampleGoal.getStatus());
        assertTrue(milestones.get(3).isAchieved());
    }

    @Test
    @DisplayName("Should prevent overfunding when contribution exceeds remaining target")
    void testRecordContribution_PreventsOverfunding() {
        sampleGoal.setCurrentAmount(new BigDecimal("9000.00")); // Remaining is 1,000

        when(savingsGoalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleGoal));

        // Attempting to contribute 1,500
        OverfundingException ex = assertThrows(OverfundingException.class, () ->
                contributionService.recordContribution(
                        1L, "Musarrat", "Musarrat", new BigDecimal("1500.00"), "Too much"
                )
        );
        assertTrue(ex.getMessage().contains("exceeds the remaining target"));
        verify(contributionRepository, never()).save(any(Contribution.class));
    }
}
