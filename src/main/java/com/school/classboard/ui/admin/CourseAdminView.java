package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Course;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.CourseService;
import com.school.classboard.ui.ClassSession;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 后台：班级课表维护（需要登录）。
 *
 * <p>以「节次 × 星期」表格展示整周课表，点击任意格子即可编辑该节课；
 * 同一班级、星期、节次重复保存会覆盖原记录，不会产生重复行。
 */
@Route(value = "admin/courses", layout = MainLayout.class)
@PageTitle("课表管理")
@PermitAll
public class CourseAdminView extends Composite<VerticalLayout> {

    /** 默认展示的节次数，实际会取课表中最大的节次 */
    private static final int DEFAULT_PERIODS = 8;
    private static final int MAX_PERIOD = 12;

    private final ClassInfoService classInfoService;
    private final CourseService courseService;

    private final ComboBox<ClassInfo> classSelect = new ComboBox<>("班级");
    private final Grid<PeriodRow> grid = new Grid<>();

    private ClassInfo currentClass;

    public CourseAdminView(ClassInfoService classInfoService, CourseService courseService) {
        this.classInfoService = classInfoService;
        this.courseService = courseService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("课表管理");
        title.getStyle().set("margin", "0");

        Button add = new Button("新增课程", e -> openSlotEditor(1, 1));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        classSelect.setWidth("220px");
        classSelect.setItemLabelGenerator(ClassInfo::getName);
        classSelect.addValueChangeListener(e -> {
            currentClass = e.getValue();
            refreshGrid();
        });

        HorizontalLayout toolbar = new HorizontalLayout(classSelect, add);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(title, toolbar);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        Span hint = new Span("点击课表格子即可编辑该节课；留空表示当天该节没有课。");
        hint.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");
        root.add(hint);

        buildColumns();
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        root.add(grid);

        List<ClassInfo> classes = classInfoService.findAll();
        classSelect.setItems(classes);
        if (classes.isEmpty()) {
            grid.setItems(List.of());
        } else {
            Long sessionClassId = ClassSession.getClassId();
            ClassInfo initial = classes.stream()
                    .filter(c -> c.getId().equals(sessionClassId))
                    .findFirst()
                    .orElse(classes.get(0));
            classSelect.setValue(initial);
            currentClass = initial;
            refreshGrid();
        }
    }

    private void buildColumns() {
        grid.addColumn(r -> "第 " + r.period + " 节")
                .setHeader("节次")
                .setAutoWidth(true)
                .setFlexGrow(0);
        for (int day = 1; day <= 7; day++) {
            final int d = day;
            grid.addComponentColumn(row -> slotCell(row, d))
                    .setHeader(CourseService.dayName(day))
                    .setAutoWidth(true)
                    .setFlexGrow(1);
        }
    }

    /** 一个课表格子：有课显示科目/教师/时间，没课显示「＋ 添加」 */
    private Component slotCell(PeriodRow row, int day) {
        Course course = row.days[day];

        VerticalLayout lines = new VerticalLayout();
        lines.setPadding(false);
        lines.setSpacing(false);
        lines.getStyle().set("line-height", "1.3");

        if (course == null || course.getSubject() == null || course.getSubject().isBlank()) {
            Span empty = new Span("＋ 添加");
            empty.getStyle().set("color", "var(--lumo-tertiary-text-color)").set("font-size", "0.85rem");
            lines.add(empty);
        } else {
            Span subject = new Span(course.getSubject());
            subject.getStyle().set("font-weight", "600").set("font-size", "0.95rem");
            lines.add(subject);

            String meta = joinMeta(course);
            if (!meta.isEmpty()) {
                Span detail = new Span(meta);
                detail.getStyle()
                        .set("font-size", "0.78rem")
                        .set("color", "var(--lumo-secondary-text-color)");
                lines.add(detail);
            }
        }

        Div cell = new Div(lines);
        cell.setWidthFull();
        cell.getStyle()
                .set("cursor", "pointer")
                .set("padding", "0.25rem 0.4rem")
                .set("border-radius", "6px")
                .set("min-height", "48px")
                .set("border", course == null ? "1px dashed var(--lumo-contrast-20pct)" : "1px solid transparent");
        cell.addClickListener(e -> openSlotEditor(row.period, day));
        return cell;
    }

    private String joinMeta(Course course) {
        StringBuilder meta = new StringBuilder();
        if (course.getTeacher() != null && !course.getTeacher().isBlank()) {
            meta.append(course.getTeacher());
        }
        if (course.getStartTime() != null && !course.getStartTime().isBlank()) {
            if (meta.length() > 0) {
                meta.append(" · ");
            }
            meta.append(course.getStartTime());
            if (course.getEndTime() != null && !course.getEndTime().isBlank()) {
                meta.append("-").append(course.getEndTime());
            }
        }
        return meta.toString();
    }

    private void refreshGrid() {
        if (currentClass == null) {
            grid.setItems(List.of());
            return;
        }
        List<Course> weekly = courseService.findWeekly(currentClass.getId());
        int maxPeriod = DEFAULT_PERIODS;
        for (Course c : weekly) {
            if (c.getPeriod() != null) {
                maxPeriod = Math.max(maxPeriod, c.getPeriod());
            }
        }
        Map<Integer, PeriodRow> rows = new TreeMap<>();
        for (int period = 1; period <= maxPeriod; period++) {
            rows.put(period, new PeriodRow(period));
        }
        for (Course c : weekly) {
            PeriodRow row = c.getPeriod() == null ? null : rows.get(c.getPeriod());
            if (row != null && c.getDayOfWeek() != null) {
                row.put(c.getDayOfWeek(), c);
            }
        }
        grid.setItems(rows.values());
    }

    private void openSlotEditor(int period, int dayOfWeek) {
        if (currentClass == null) {
            return;
        }
        Course existing = courseService.findSlot(currentClass.getId(), dayOfWeek, period).orElse(null);
        boolean editing = existing != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle((editing ? "编辑课程 · " : "新增课程 · ") + currentClass.getName());
        dialog.setWidth("480px");

        ComboBox<Integer> day = new ComboBox<>("星期");
        day.setItems(1, 2, 3, 4, 5, 6, 7);
        day.setItemLabelGenerator(CourseService::dayName);
        day.setValue(dayOfWeek);

        IntegerField periodField = new IntegerField("节次");
        periodField.setMin(1);
        periodField.setMax(MAX_PERIOD);
        periodField.setStepButtonsVisible(true);
        periodField.setValue(period);

        // 已存在的格子不允许改星期/节次，否则会留下一条旧记录
        day.setReadOnly(editing);
        periodField.setReadOnly(editing);

        TextField subject = new TextField("科目");
        subject.setPlaceholder("如：数学");
        TextField teacher = new TextField("任课教师");
        teacher.setPlaceholder("如：张老师");
        TextField start = new TextField("开始时间");
        start.setPlaceholder("08:00");
        TextField end = new TextField("结束时间");
        end.setPlaceholder("08:45");

        VerticalLayout form = new VerticalLayout(day, periodField, subject, teacher, start, end);
        form.setPadding(false);
        form.setSpacing(true);
        form.setWidthFull();

        Binder<Course> binder = new Binder<>(Course.class);
        binder.forField(subject).asRequired("请填写科目").bind(Course::getSubject, Course::setSubject);
        binder.forField(teacher).bind(Course::getTeacher, Course::setTeacher);
        binder.forField(start)
                .withValidator(v -> v == null || v.isBlank() || v.matches("\\d{1,2}:\\d{2}"), "时间格式如 08:00")
                .bind(Course::getStartTime, Course::setStartTime);
        binder.forField(end)
                .withValidator(v -> v == null || v.isBlank() || v.matches("\\d{1,2}:\\d{2}"), "时间格式如 08:45")
                .bind(Course::getEndTime, Course::setEndTime);
        binder.setBean(editing ? existing : new Course());

        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (!binder.validate().isOk() || day.getValue() == null || periodField.getValue() == null) {
                return;
            }
            Course payload = new Course();
            payload.setClassId(currentClass.getId());
            payload.setDayOfWeek(day.getValue());
            payload.setPeriod(periodField.getValue());
            payload.setSubject(subject.getValue());
            payload.setTeacher(teacher.getValue());
            payload.setStartTime(start.getValue());
            payload.setEndTime(end.getValue());
            courseService.saveForSlot(payload);
            refreshGrid();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());

        if (editing) {
            Button clear = new Button("清空该课", e -> {
                courseService.delete(existing.getId());
                refreshGrid();
                dialog.close();
            });
            clear.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            dialog.getFooter().add(clear);
        }
        dialog.getFooter().add(cancel, save);

        dialog.open();
    }

    /** 课表行：一个节次对应一周七天的课程 */
    static class PeriodRow {
        private final int period;
        private final Course[] days = new Course[8];

        PeriodRow(int period) {
            this.period = period;
        }

        void put(int dayOfWeek, Course course) {
            if (dayOfWeek >= 1 && dayOfWeek <= 7) {
                days[dayOfWeek] = course;
            }
        }
    }
}
