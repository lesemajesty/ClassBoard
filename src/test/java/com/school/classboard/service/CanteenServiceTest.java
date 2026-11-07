package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.Meal;
import com.school.classboard.repository.CanteenMenuRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CanteenServiceTest {

    @Mock
    private CanteenMenuRepository repository;

    @InjectMocks
    private CanteenService service;

    @Test
    void currentWeekStartIsMonday() {
        assertEquals(DayOfWeek.MONDAY, CanteenService.currentWeekStart().getDayOfWeek());
    }

    @Test
    void syncCopiesPreviousWeekSlotsIntoCurrentWeek() {
        LocalDate week = LocalDate.of(2026, 10, 19);
        CanteenMenu previous = new CanteenMenu();
        previous.setWeekStart(week.minusWeeks(1));
        previous.setDayOfWeek(1);
        previous.setMeal(Meal.LUNCH);
        previous.setDishes("红烧鸡腿 / 番茄炒蛋");
        previous.setPrice("12 元");

        when(repository.findByWeekStartOrderByDayOfWeekAscMealAsc(week.minusWeeks(1)))
                .thenReturn(List.of(previous));
        when(repository.findByWeekStartAndDayOfWeekAndMeal(week, 1, Meal.LUNCH))
                .thenReturn(Optional.empty());

        int copied = service.syncFromPreviousWeek(week);

        assertEquals(1, copied);
        ArgumentCaptor<CanteenMenu> captor = ArgumentCaptor.forClass(CanteenMenu.class);
        verify(repository).save(captor.capture());
        CanteenMenu saved = captor.getValue();
        assertEquals(week, saved.getWeekStart());
        assertEquals(1, saved.getDayOfWeek().intValue());
        assertEquals(Meal.LUNCH, saved.getMeal());
        assertEquals("红烧鸡腿 / 番茄炒蛋", saved.getDishes());
    }

    @Test
    void syncWithEmptyPreviousWeekDoesNothing() {
        LocalDate week = LocalDate.of(2026, 10, 19);
        when(repository.findByWeekStartOrderByDayOfWeekAscMealAsc(week.minusWeeks(1)))
                .thenReturn(List.of());

        int copied = service.syncFromPreviousWeek(week);

        assertEquals(0, copied);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }
}
