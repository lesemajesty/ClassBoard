package com.school.classboard.ui.admin;

import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.Meal;
import com.school.classboard.service.CanteenService;
import com.school.classboard.service.CourseService;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 后台：食堂周菜单维护，支持从上一周同步。
 */
@Route(value = "admin/canteen", layout = MainLayout.class)
@PageTitle("食堂菜单")
@PermitAll
public class CanteenAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final CanteenService service;

    private final DatePicker weekPicker = new DatePicker("周");
    private final Grid<WeekRow> grid = new Grid<>();

    public CanteenAdminView(CanteenService service) {
        this.service = service;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("食堂菜单");
        title.getStyle().set("margin", "0");

        weekPicker.setValue(CanteenService.currentWeekStart());
        weekPicker.setWidth("180px");
        weekPicker.setHelperText("选择该周任意一天即可");
        weekPicker.addValueChangeListener(e -> refresh());

        Button sync = new Button("同步上周菜单", e -> syncPreviousWeek());
        sync.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(weekPicker, sync);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        HorizontalLayout header = new HorizontalLayout(title, toolbar);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        Span hint = new Span("点击任意餐次即可编辑菜品；「同步上周菜单」会把上周内容复制到当前周。");
        hint.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");
        root.add(hint);

        grid.addColumn(row -> CourseService.dayName(row.day)).setHeader("星期").setAutoWidth(true).setFlexGrow(0);
        for (Meal meal : Meal.values()) {
            grid.addComponentColumn(row -> mealCell(row, meal))
                    .setHeader(meal.getLabel())
                    .setFlexGrow(1);
        }
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        root.add(grid);
        root.expand(grid);

        refresh();
    }

    private LocalDate weekStart() {
        LocalDate value = weekPicker.getValue() == null ? LocalDate.now() : weekPicker.getValue();
        return value.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private Component mealCell(WeekRow row, Meal meal) {
        CanteenMenu menu = row.meals.get(meal);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        Span dishes = new Span(menu == null || menu.getDishes() == null ? "＋ 添加" : menu.getDishes());
        dishes.getStyle()
                .set("font-size", "0.88rem")
                .set("color", menu == null ? "var(--lumo-tertiary-text-color)" : "var(--lumo-body-text-color)");
        content.add(dishes);

        if (menu != null && menu.getPrice() != null) {
            Span price = new Span(menu.getPrice());
            price.getStyle().set("font-size", "0.78rem").set("color", "#b45309");
            content.add(price);
        }

        Div cell = new Div(content);
        cell.setWidthFull();
        cell.getStyle()
                .set("cursor", "pointer")
                .set("padding", "0.35rem 0.5rem")
                .set("border-radius", "8px")
                .set("min-height", "52px")
                .set("border", menu == null ? "1px dashed var(--lumo-contrast-20pct)" : "1px solid transparent");
        cell.addClickListener(e -> openEditor(row.day, meal, menu));
        return cell;
    }

    private void openEditor(int day, Meal meal, CanteenMenu existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(CourseService.dayName(day) + " · " + meal.getLabel());
        dialog.setWidth("480px");

        TextArea dishes = new TextArea("菜品");
        dishes.setWidthFull();
        dishes.setHeight("110px");
        dishes.setPlaceholder("用 / 分隔，如：红烧鸡腿 / 番茄炒蛋 / 时蔬 / 米饭");
        TextField price = new TextField("价格");
        price.setPlaceholder("如：12 元");
        TextField note = new TextField("备注");
        note.setPlaceholder("可选，如：含清真窗口");

        if (existing != null) {
            dishes.setValue(existing.getDishes() == null ? "" : existing.getDishes());
            price.setValue(existing.getPrice() == null ? "" : existing.getPrice());
            note.setValue(existing.getNote() == null ? "" : existing.getNote());
        }

        VerticalLayout form = new VerticalLayout(dishes, price, note);
        form.setPadding(false);
        form.setWidthFull();
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (dishes.isEmpty()) {
                dishes.setInvalid(true);
                dishes.setErrorMessage("请填写菜品");
                return;
            }
            service.saveSlot(weekStart(), day, meal, dishes.getValue(), price.getValue(), note.getValue());
            refresh();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void syncPreviousWeek() {
        LocalDate current = weekStart();
        int copied = service.syncFromPreviousWeek(current);
        if (copied == 0) {
            Notification.show("上一周没有菜单可同步");
        } else {
            Notification.show("已从 " + current.minusWeeks(1).format(FMT) + " 那周同步 " + copied + " 个餐次");
        }
        refresh();
    }

    private void refresh() {
        LocalDate start = weekStart();
        List<CanteenMenu> menus = service.weekMenu(start);
        List<WeekRow> rows = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            WeekRow row = new WeekRow(day);
            for (CanteenMenu menu : menus) {
                if (menu.getDayOfWeek() != null && menu.getDayOfWeek() == day) {
                    row.meals.put(menu.getMeal(), menu);
                }
            }
            rows.add(row);
        }
        grid.setItems(rows);
    }

    /** 一周中的一天 */
    private static class WeekRow {
        private final int day;
        private final Map<Meal, CanteenMenu> meals = new EnumMap<>(Meal.class);

        WeekRow(int day) {
            this.day = day;
        }
    }
}
