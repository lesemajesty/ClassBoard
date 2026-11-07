package com.school.classboard.service;

import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.Meal;
import com.school.classboard.repository.CanteenMenuRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 食堂菜单：按周维护，支持从上一周同步。
 */
@Service
@Transactional(readOnly = true)
public class CanteenService {

    private final CanteenMenuRepository repository;

    public CanteenService(CanteenMenuRepository repository) {
        this.repository = repository;
    }

    /** 本周周一 */
    public static LocalDate currentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public List<CanteenMenu> weekMenu(LocalDate weekStart) {
        return repository.findByWeekStartOrderByDayOfWeekAscMealAsc(
                weekStart == null ? currentWeekStart() : weekStart);
    }

    public List<CanteenMenu> todayMenu() {
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        return weekMenu(currentWeekStart()).stream()
                .filter(m -> m.getDayOfWeek() != null && m.getDayOfWeek() == dayOfWeek)
                .toList();
    }

    public Optional<CanteenMenu> findSlot(LocalDate weekStart, int dayOfWeek, Meal meal) {
        return repository.findByWeekStartAndDayOfWeekAndMeal(weekStart, dayOfWeek, meal);
    }

    @Transactional
    public CanteenMenu saveSlot(LocalDate weekStart, int dayOfWeek, Meal meal,
                                String dishes, String price, String note) {
        CanteenMenu menu = repository
                .findByWeekStartAndDayOfWeekAndMeal(weekStart, dayOfWeek, meal)
                .orElseGet(() -> {
                    CanteenMenu created = new CanteenMenu();
                    created.setWeekStart(weekStart);
                    created.setDayOfWeek(dayOfWeek);
                    created.setMeal(meal);
                    return created;
                });
        menu.setDishes(dishes);
        menu.setPrice(price);
        menu.setNote(note);
        return repository.save(menu);
    }

    /**
     * 从上一周同步菜单到指定周：食堂每周菜单大多沿用，管理员只需微调。
     *
     * @return 同步的餐次数量
     */
    @Transactional
    public int syncFromPreviousWeek(LocalDate weekStart) {
        LocalDate previousWeek = weekStart.minusWeeks(1);
        List<CanteenMenu> previous = repository
                .findByWeekStartOrderByDayOfWeekAscMealAsc(previousWeek);
        for (CanteenMenu old : previous) {
            saveSlot(weekStart, old.getDayOfWeek(), old.getMeal(),
                    old.getDishes(), old.getPrice(), old.getNote());
        }
        return previous.size();
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
