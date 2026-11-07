package com.school.classboard.repository;

import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.Meal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteenMenuRepository extends JpaRepository<CanteenMenu, Long> {

    List<CanteenMenu> findByWeekStartOrderByDayOfWeekAscMealAsc(LocalDate weekStart);

    Optional<CanteenMenu> findByWeekStartAndDayOfWeekAndMeal(LocalDate weekStart, Integer dayOfWeek, Meal meal);
}
