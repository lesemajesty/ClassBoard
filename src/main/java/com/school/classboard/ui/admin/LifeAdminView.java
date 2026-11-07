package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.CountdownEvent;
import com.school.classboard.domain.DutyRoster;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.CountdownService;
import com.school.classboard.service.CourseService;
import com.school.classboard.service.DutyService;
import com.school.classboard.ui.ClassSession;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台：倒计时与值日表维护。
 */
@Route(value = "admin/life", layout = MainLayout.class)
@PageTitle("班级生活")
@PermitAll
public class LifeAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CountdownService countdownService;
    private final DutyService dutyService;
    private final ClassInfoService classInfoService;

    private final Grid<CountdownEvent> countdownGrid = new Grid<>(CountdownEvent.class, false);
    private final ComboBox<ClassInfo> classSelect = new ComboBox<>("班级");
    private final Grid<DutyRow> dutyGrid = new Grid<>();

    private ClassInfo currentClass;

    public LifeAdminView(CountdownService countdownService,
                         DutyService dutyService,
                         ClassInfoService classInfoService) {
        this.countdownService = countdownService;
        this.dutyService = dutyService;
        this.classInfoService = classInfoService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("班级生活");
        title.getStyle().set("margin", "0");
        root.add(title);

        H3 countdownTitle = new H3("考试 / 活动倒计时");
        countdownTitle.getStyle().set("margin", "0.5rem 0 0");

        Button addCountdown = new Button("新增倒计时", e -> openCountdownEditor(new CountdownEvent()));
        addCountdown.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout countdownHeader = new HorizontalLayout(countdownTitle, addCountdown);
        countdownHeader.setWidthFull();
        countdownHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        countdownHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        countdownGrid.addColumn(CountdownEvent::getName).setHeader("名称").setAutoWidth(true);
        countdownGrid.addColumn(event -> event.getEventDate().format(FMT)).setHeader("日期").setAutoWidth(true);
        countdownGrid.addColumn(this::remaining).setHeader("剩余").setAutoWidth(true);
        countdownGrid.addColumn(event -> event.getNote() == null ? "" : event.getNote())
                .setHeader("备注").setAutoWidth(true);
        countdownGrid.addComponentColumn(this::countdownActions).setHeader("操作").setAutoWidth(true);
        countdownGrid.setAllRowsVisible(true);

        H3 dutyTitle = new H3("值日表");
        dutyTitle.getStyle().set("margin", "1.5rem 0 0");

        classSelect.setWidth("200px");
        classSelect.setItemLabelGenerator(ClassInfo::getName);
        classSelect.addValueChangeListener(e -> {
            currentClass = e.getValue();
            refreshDuty();
        });

        HorizontalLayout dutyToolbar = new HorizontalLayout(classSelect);
        dutyToolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        dutyGrid.addColumn(row -> CourseService.dayName(row.day)).setHeader("星期").setAutoWidth(true).setFlexGrow(0);
        dutyGrid.addColumn(row -> row.roster == null || row.roster.getStudents() == null
                        ? "未安排" : row.roster.getStudents())
                .setHeader("值日生").setAutoWidth(true);
        dutyGrid.addColumn(row -> row.roster == null || row.roster.getTask() == null
                        ? "" : row.roster.getTask())
                .setHeader("值日内容").setAutoWidth(true);
        dutyGrid.addComponentColumn(this::dutyAction).setHeader("操作").setAutoWidth(true);
        dutyGrid.setAllRowsVisible(true);

        root.add(countdownHeader, countdownGrid, dutyTitle, dutyToolbar, dutyGrid);

        List<ClassInfo> classes = classInfoService.findAll();
        classSelect.setItems(classes);
        if (!classes.isEmpty()) {
            Long sessionClassId = ClassSession.getClassId();
            ClassInfo initial = classes.stream()
                    .filter(c -> c.getId().equals(sessionClassId))
                    .findFirst()
                    .orElse(classes.get(0));
            classSelect.setValue(initial);
            currentClass = initial;
        }

        refreshCountdown();
        refreshDuty();
    }

    private String remaining(CountdownEvent event) {
        long days = countdownService.daysUntil(event);
        if (days < 0) {
            return "已结束";
        }
        return days == 0 ? "就是今天" : days + " 天";
    }

    private Component countdownActions(CountdownEvent event) {
        Button edit = new Button("编辑", e -> openCountdownEditor(event));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Button delete = new Button("删除", e -> {
            countdownService.delete(event.getId());
            refreshCountdown();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    private Component dutyAction(DutyRow row) {
        Button edit = new Button(row.roster == null ? "安排" : "编辑", e -> openDutyEditor(row.day, row.roster));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        return edit;
    }

    private void refreshCountdown() {
        countdownGrid.setItems(countdownService.findAll());
    }

    private void refreshDuty() {
        List<DutyRow> rows = new ArrayList<>();
        if (currentClass == null) {
            dutyGrid.setItems(rows);
            return;
        }
        List<DutyRoster> rosters = dutyService.forClass(currentClass.getId());
        for (int day = 1; day <= 5; day++) {
            DutyRow row = new DutyRow(day);
            for (DutyRoster roster : rosters) {
                if (roster.getDayOfWeek() != null && roster.getDayOfWeek() == day) {
                    row.roster = roster;
                }
            }
            rows.add(row);
        }
        dutyGrid.setItems(rows);
    }

    private void openCountdownEditor(CountdownEvent event) {
        boolean isNew = event.getId() == null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "新增倒计时" : "编辑倒计时");
        dialog.setWidth("440px");

        TextField name = new TextField("名称");
        name.setPlaceholder("如：期中考试");
        DatePicker date = new DatePicker("日期");
        TextField note = new TextField("备注");
        note.setWidthFull();

        if (event.getEventDate() != null) {
            date.setValue(event.getEventDate());
        }
        if (isNew) {
            name.setValue("");
        } else {
            name.setValue(event.getName());
            note.setValue(event.getNote() == null ? "" : event.getNote());
        }

        VerticalLayout form = new VerticalLayout(name, date, note);
        form.setPadding(false);
        form.setWidthFull();
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (name.isEmpty() || date.getValue() == null) {
                name.setInvalid(name.isEmpty());
                date.setInvalid(date.getValue() == null);
                return;
            }
            event.setName(name.getValue().trim());
            event.setEventDate(date.getValue());
            event.setNote(note.getValue());
            countdownService.save(event);
            refreshCountdown();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openDutyEditor(int day, DutyRoster existing) {
        if (currentClass == null) {
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(CourseService.dayName(day) + "值日安排");
        dialog.setWidth("440px");

        TextField students = new TextField("值日生");
        students.setPlaceholder("用、分隔，如：王雨桐、李泽宇");
        students.setWidthFull();
        TextField task = new TextField("值日内容");
        task.setPlaceholder("如：扫地、擦黑板、倒垃圾");
        task.setWidthFull();

        if (existing != null) {
            students.setValue(existing.getStudents() == null ? "" : existing.getStudents());
            task.setValue(existing.getTask() == null ? "" : existing.getTask());
        }

        VerticalLayout form = new VerticalLayout(students, task);
        form.setPadding(false);
        form.setWidthFull();
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (students.isEmpty()) {
                students.setInvalid(true);
                students.setErrorMessage("请填写值日生");
                return;
            }
            dutyService.save(currentClass.getId(), day, students.getValue(), task.getValue());
            refreshDuty();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    /** 值日表中的一行 */
    private static class DutyRow {
        private final int day;
        private DutyRoster roster;

        DutyRow(int day) {
            this.day = day;
        }
    }
}
