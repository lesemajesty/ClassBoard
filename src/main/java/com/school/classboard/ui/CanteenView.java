package com.school.classboard.ui;

import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.Meal;
import com.school.classboard.service.CanteenService;
import com.school.classboard.service.CourseService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 食堂菜单页：按周展示早/午/晚餐。
 */
@Route(value = "canteen", layout = MainLayout.class)
@PageTitle("食堂菜单")
@AnonymousAllowed
public class CanteenView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd");

    public CanteenView(CanteenService canteenService) {
        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        LocalDate weekStart = CanteenService.currentWeekStart();
        H2 title = new H2("食堂菜单 · 本周");
        title.getStyle().set("margin", "0");

        Span range = new Span("本周 " + weekStart.format(FMT) + " 至 " + weekStart.plusDays(6).format(FMT));
        range.getStyle().set("color", "var(--lumo-secondary-text-color)");

        List<DayMenu> rows = new ArrayList<>();
        List<CanteenMenu> menus = canteenService.weekMenu(weekStart);
        for (int day = 1; day <= 7; day++) {
            DayMenu row = new DayMenu(day);
            for (CanteenMenu menu : menus) {
                if (menu.getDayOfWeek() != null && menu.getDayOfWeek() == day) {
                    row.meals.put(menu.getMeal(), menu);
                }
            }
            rows.add(row);
        }

        Grid<DayMenu> grid = new Grid<>();
        grid.addColumn(row -> CourseService.dayName(row.day)).setHeader("星期").setAutoWidth(true).setFlexGrow(0);
        for (Meal meal : Meal.values()) {
            grid.addComponentColumn(row -> mealCell(row.meals.get(meal)))
                    .setHeader(meal.getLabel())
                    .setFlexGrow(1);
        }
        grid.setItems(rows);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        root.add(title, range, grid);
        root.expand(grid);
    }

    private Component mealCell(CanteenMenu menu) {
        VerticalLayout cell = new VerticalLayout();
        cell.setPadding(false);
        cell.setSpacing(false);
        cell.getStyle().set("line-height", "1.4");
        if (menu == null) {
            cell.add(new Span("—"));
            return cell;
        }
        Span dishes = new Span(menu.getDishes() == null ? "" : menu.getDishes());
        dishes.getStyle().set("font-size", "0.9rem");
        cell.add(dishes);
        if (menu.getPrice() != null) {
            Span price = new Span(menu.getPrice());
            price.getStyle().set("font-size", "0.8rem").set("color", "#b45309");
            cell.add(price);
        }
        return cell;
    }

    /** 一天的菜单 */
    private static class DayMenu {
        private final int day;
        private final Map<Meal, CanteenMenu> meals = new EnumMap<>(Meal.class);

        DayMenu(int day) {
            this.day = day;
        }
    }
}
