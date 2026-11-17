package com.chipin.chipin;

import com.chipin.chipin.dto.GoalResponseDTO;
import com.chipin.chipin.model.GoalStatus;
import com.chipin.chipin.model.Milestone;
import com.chipin.chipin.model.SavingsGoal;
import com.chipin.chipin.repository.MilestoneRepository;
import com.chipin.chipin.repository.SavingsGoalRepository;
import com.chipin.chipin.service.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Feature 15: GoalService Milestones & Creation Tests")
class GoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @InjectMocks
    private GoalService goalService;

    private LocalDate futureDeadline;

    @BeforeEach
    void setUp() {
        futureDeadline = LocalDate.now().plusMonths(3);
    }

    @Test
    @DisplayName("Should create goal and atomically initialize 4 standard milestones (25%, 50%, 75%, 100%)")
    void testCreateGoal_InitializesMilestones() {
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

        SavingsGoal created = goalService.createGoal(
                "group_01",
                "Mufliha",
                "Cox's Bazar Trip",
                "Annual retreat",
                new BigDecimal("40000.00"),
                futureDeadline
        );

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("Cox's Bazar Trip", created.getTitle());
        assertEquals(GoalStatus.ACTIVE, created.getStatus());
        assertEquals(new BigDecimal("40000.00"), created.getTargetAmount());
        assertEquals(BigDecimal.ZERO, created.getCurrentAmount());

        // Invariant: exactly 4 milestones created
        assertEquals(4, created.getMilestones().size());
        assertEquals(25, created.getMilestones().get(0).getPercentage());
        assertEquals(new BigDecimal("10000.00"), created.getMilestones().get(0).getTargetAmountSnapshot());
        assertEquals(50, created.getMilestones().get(1).getPercentage());
        assertEquals(new BigDecimal("20000.00"), created.getMilestones().get(1).getTargetAmountSnapshot());
        assertEquals(75, created.getMilestones().get(2).getPercentage());
        assertEquals(new BigDecimal("30000.00"), created.getMilestones().get(2).getTargetAmountSnapshot());
        assertEquals(100, created.getMilestones().get(3).getPercentage());
        assertEquals(new BigDecimal("40000.00"), created.getMilestones().get(3).getTargetAmountSnapshot());

        verify(savingsGoalRepository).save(any(SavingsGoal.class));
    }

    @Test
    @DisplayName("Should reject goal creation with deadline in the past")
    void testCreateGoal_PastDeadline_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                goalService.createGoal(
                        "group_01", "Mufliha", "Past Goal", null,
                        new BigDecimal("5000.00"), LocalDate.now().minusDays(1)
                )
        );
        assertTrue(ex.getMessage().contains("Deadline date cannot be in the past"));
    }

    @Test
    @DisplayName("Should reject goal creation with zero or negative target amount")
    void testCreateGoal_NonPositiveAmount_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                goalService.createGoal(
                        "group_01", "Mufliha", "Bad Target", null,
                        BigDecimal.ZERO, futureDeadline
                )
        );
    }
}
