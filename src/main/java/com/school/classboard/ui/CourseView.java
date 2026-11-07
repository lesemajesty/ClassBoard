package com.school.classboard.ui;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Course;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.CourseService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 班级课表页：以「节次 × 星期」表格形式展示所选班级的整周课表。
 */
@Route(value = "courses", layout = MainLayout.class)
@PageTitle("班级课表")
@AnonymousAllowed
public class CourseView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final ClassInfoService classInfoService;
    private final CourseService courseService;

    public CourseView(ClassInfoService classInfoService, CourseService courseService) {
        this.classInfoService = classInfoService;
        this.courseService = courseService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        ClassInfo clazz = currentClass();
        if (clazz == null) {
            // 未选择班级，由 beforeEnter 跳转到登录页
            return;
        }

        H2 title = new H2("班级课表 · " + clazz.getName());
        title.getStyle().set("margin", "0");
        root.add(title);

        List<Course> weekly = courseService.findWeekly(clazz.getId());
        Map<Integer, PeriodRow> rows = new TreeMap<>();
        Map<Integer, String> timeSlots = new TreeMap<>();
        for (Course c : weekly) {
            PeriodRow row = rows.computeIfAbsent(c.getPeriod(), PeriodRow::new);
            row.put(c.getDayOfWeek(), c);
            timeSlots.putIfAbsent(c.getPeriod(), format(c));
        }
        rows.values().forEach(r -> r.time = timeSlots.getOrDefault(r.period, ""));

        Grid<PeriodRow> grid = new Grid<>();
        grid.addColumn(r -> "第 " + r.period + " 节").setHeader("节次").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> r.time).setHeader("时间").setAutoWidth(true).setFlexGrow(0);
        for (int day = 1; day <= 5; day++) {
            final int d = day;
            grid.addComponentColumn(r -> subjectCell(r.day(d)))
                    .setHeader(CourseService.dayName(day))
                    .setAutoWidth(true)
                    .setFlexGrow(1);
        }
        grid.setItems(rows.values());
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);
    }

    /** 课表格子：科目 + 任课教师，分两行显示（纯文本里的 \n 在 Grid 中不会换行） */
    private Component subjectCell(Course course) {
        VerticalLayout cell = new VerticalLayout();
        cell.setPadding(false);
        cell.setSpacing(false);
        cell.getStyle().set("line-height", "1.35");
        if (course == null) {
            return cell;
        }
        Span subject = new Span(text(course.getSubject()));
        subject.getStyle().set("font-weight", "600");
        cell.add(subject);
        if (course.getTeacher() != null && !course.getTeacher().isBlank()) {
            Span teacher = new Span(course.getTeacher());
            teacher.getStyle()
                    .set("font-size", "0.8rem")
                    .set("color", "var(--lumo-secondary-text-color)");
            cell.add(teacher);
        }
        return cell;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (currentClass() == null) {
            event.rerouteTo(LoginView.class);
        }
    }

    private ClassInfo currentClass() {
        Long classId = ClassSession.getClassId();
        return classId == null ? null : classInfoService.findById(classId).orElse(null);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String format(Course c) {
        if (c.getStartTime() == null && c.getEndTime() == null) {
            return "";
        }
        return (c.getStartTime() == null ? "" : c.getStartTime())
                + (c.getEndTime() == null ? "" : " - " + c.getEndTime());
    }

    /** 课表行：一个节次对应周一到周五的课程 */
    static class PeriodRow {
        private final int period;
        private String time = "";
        private final Course[] days = new Course[8];

        PeriodRow(int period) {
            this.period = period;
        }

        void put(int dayOfWeek, Course course) {
            if (dayOfWeek >= 1 && dayOfWeek <= 7) {
                days[dayOfWeek] = course;
            }
        }

        Course day(int dayOfWeek) {
            return dayOfWeek >= 1 && dayOfWeek <= 7 ? days[dayOfWeek] : null;
        }
    }
}
