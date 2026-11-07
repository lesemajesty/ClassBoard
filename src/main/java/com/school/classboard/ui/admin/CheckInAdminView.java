package com.school.classboard.ui.admin;

import com.school.classboard.domain.CheckInRecord;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.service.CheckInService;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台：签到记录查询。按日期、班级筛选，并显示当日统计。
 */
@Route(value = "admin/checkins", layout = MainLayout.class)
@PageTitle("签到记录")
@PermitAll
public class CheckInAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CheckInService service;
    private final ClassInfoService classInfoService;

    private final DatePicker date = new DatePicker("日期");
    private final ComboBox<ClassInfo> classSelect = new ComboBox<>("班级");
    private final Span stats = new Span();
    private final Grid<CheckInRecord> grid = new Grid<>(CheckInRecord.class, false);

    private Map<Long, ClassInfo> classById = Map.of();

    public CheckInAdminView(CheckInService service, ClassInfoService classInfoService) {
        this.service = service;
        this.classInfoService = classInfoService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("签到记录");
        title.getStyle().set("margin", "0");

        date.setValue(LocalDate.now());
        date.setWidth("160px");
        date.addValueChangeListener(e -> refresh());

        classSelect.setWidth("200px");
        classSelect.setItemLabelGenerator(ClassInfo::getName);
        classSelect.setClearButtonVisible(true);
        classSelect.setPlaceholder("全部班级");
        classSelect.addValueChangeListener(e -> refresh());

        stats.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout toolbar = new HorizontalLayout(date, classSelect, stats);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(title, toolbar);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        grid.addColumn(r -> r.getCheckedAt().format(TIME_FMT)).setHeader("时间").setAutoWidth(true);
        grid.addColumn(CheckInRecord::getStudentName).setHeader("姓名").setAutoWidth(true);
        grid.addColumn(this::className).setHeader("班级").setAutoWidth(true);
        grid.addComponentColumn(this::typeBadge).setHeader("类型").setAutoWidth(true);
        grid.addColumn(r -> r.isLate() ? "迟到" : "").setHeader("迟到").setAutoWidth(true);
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);

        List<ClassInfo> classes = classInfoService.findAll();
        classSelect.setItems(classes);
        classById = classes.stream().collect(Collectors.toMap(ClassInfo::getId, Function.identity()));

        refresh();
    }

    private String className(CheckInRecord record) {
        ClassInfo clazz = classById.get(record.getClassId());
        return clazz == null ? "—" : clazz.getName();
    }

    private Component typeBadge(CheckInRecord record) {
        Span span = new Span(record.getType().getLabel());
        span.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.15rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", "#dbeafe")
                .set("color", "#1d4ed8");
        return span;
    }

    private void refresh() {
        LocalDate selectedDate = date.getValue() == null ? LocalDate.now() : date.getValue();
        ClassInfo clazz = classSelect.getValue();
        List<CheckInRecord> records = service.records(selectedDate, clazz == null ? null : clazz.getId());
        grid.setItems(records);

        if (clazz != null && selectedDate.equals(LocalDate.now())) {
            CheckInService.Stats today = service.stats(clazz.getId());
            stats.setText("应到 " + today.expected() + " · 实到 " + today.present()
                    + " · 迟到 " + today.late() + " · 已签退 " + today.left());
        } else {
            stats.setText("共 " + records.size() + " 条记录");
        }
    }
}
