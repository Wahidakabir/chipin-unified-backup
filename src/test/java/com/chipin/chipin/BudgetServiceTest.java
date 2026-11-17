package com.chipin.chipin;

import com.chipin.chipin.exception.BudgetNotFoundException;
import com.chipin.chipin.exception.CategoryNotFoundException;
import com.chipin.chipin.model.BudgetCategory;
import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.GroupBudget;
import com.chipin.chipin.repository.BudgetCategoryRepository;
import com.chipin.chipin.repository.GroupBudgetRepository;
import com.chipin.chipin.service.BudgetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Feature 11: BudgetService Invariants & Allocation Tests")
class BudgetServiceTest {

    @Mock
    private GroupBudgetRepository budgetRepository;

    @Mock
    private BudgetCategoryRepository categoryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BudgetService budgetService;

    private GroupBudget sampleBudget;

    @BeforeEach
    void setUp() {
        sampleBudget = new GroupBudget(
                "group_01",
                "Mufliha",
                "November Household Budget",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                new BigDecimal("25000.00")
        );
        sampleBudget.setId(1L);
        sampleBudget.setStatus(BudgetStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should create budget with ACTIVE status and valid inputs")
    void testCreateBudget_Success() {
        when(budgetRepository.save(any(GroupBudget.class))).thenAnswer(invocation -> {
            GroupBudget b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });

        GroupBudget created = budgetService.createBudget(
                "group_01",
                "Afifa",
                "Trip Budget",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15),
                new BigDecimal("10000.00")
        );

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("group_01", created.getGroupId());
        assertEquals("Trip Budget", created.getName());
        assertEquals(BudgetStatus.ACTIVE, created.getStatus());
        verify(budgetRepository, times(1)).save(any(GroupBudget.class));
    }

    @Test
    @DisplayName("Should reject budget creation when end date is before start date")
    void testCreateBudget_InvalidDateRange() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                budgetService.createBudget(
                        "group_01",
                        "Afifa",
                        "Invalid Dates",
                        LocalDate.of(2026, 12, 15),
                        LocalDate.of(2026, 12, 1),
                        new BigDecimal("10000.00")
                )
        );
        assertTrue(ex.getMessage().contains("start date cannot be after period end date"));
    }

    @Test
    @DisplayName("Should reject budget creation when total limit is zero or negative")
    void testCreateBudget_NonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                budgetService.createBudget(
                        "group_01",
                        "Afifa",
                        "Zero Limit",
                        LocalDate.of(2026, 12, 1),
                        LocalDate.of(2026, 12, 31),
                        BigDecimal.ZERO
                )
        );
    }

    @Test
    @DisplayName("Should successfully allocate category within remaining unallocated limit")
    void testAllocateCategory_Success() {
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(categoryRepository.existsByBudgetIdAndNormalizedName(1L, "groceries")).thenReturn(false);
        when(categoryRepository.findByBudgetId(1L)).thenReturn(Collections.emptyList());
        when(categoryRepository.saveAndFlush(any(BudgetCategory.class))).thenAnswer(invocation -> {
            BudgetCategory c = invocation.getArgument(0);
            c.setId(101L);
            return c;
        });

        BudgetCategory category = budgetService.allocateCategory(1L, "Groceries", new BigDecimal("5000.00"));

        assertNotNull(category);
        assertEquals(101L, category.getId());
        assertEquals("Groceries", category.getName());
        assertEquals(new BigDecimal("5000.00"), category.getAllocatedAmount());
        verify(categoryRepository, times(1)).saveAndFlush(any(BudgetCategory.class));
    }

    @Test
    @DisplayName("Should reject category allocation that exceeds remaining unallocated limit")
    void testAllocateCategory_ExceedsLimit() {
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(categoryRepository.existsByBudgetIdAndNormalizedName(1L, "huge category")).thenReturn(false);
        // Existing allocated is 20,000; total limit is 25,000 -> remaining is 5,000
        BudgetCategory existing = new BudgetCategory(sampleBudget, "Existing", new BigDecimal("20000.00"));
        when(categoryRepository.findByBudgetId(1L)).thenReturn(List.of(existing));

        // Attempting to allocate 6,000 (exceeds remaining 5,000)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                budgetService.allocateCategory(1L, "Huge Category", new BigDecimal("6000.00"))
        );
        assertTrue(ex.getMessage().contains("exceeds remaining unallocated budget limit"));
    }

    @Test
    @DisplayName("Should reject duplicate category name within same budget")
    void testAllocateCategory_DuplicateName() {
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(categoryRepository.existsByBudgetIdAndNormalizedName(1L, "groceries")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                budgetService.allocateCategory(1L, "groceries", new BigDecimal("3000.00"))
        );
        assertTrue(ex.getMessage().contains("already exists in this budget"));
    }

    @Test
    @DisplayName("Should reject modifying an archived budget")
    void testUpdateBudget_Archived_Throws() {
        sampleBudget.setStatus(BudgetStatus.ARCHIVED);
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                budgetService.updateBudget(1L, "New Name",
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                        new BigDecimal("30000.00"))
        );
        assertTrue(ex.getMessage().contains("Cannot modify an archived budget"));
    }

    @Test
    @DisplayName("Should archive an active budget successfully")
    void testArchiveBudget_Success() {
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(budgetRepository.saveAndFlush(any(GroupBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupBudget archived = budgetService.archiveBudget(1L);

        assertEquals(BudgetStatus.ARCHIVED, archived.getStatus());
        verify(budgetRepository).saveAndFlush(sampleBudget);
    }
}
