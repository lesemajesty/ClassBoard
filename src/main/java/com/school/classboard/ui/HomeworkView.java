package com.school.classboard.ui;

import com.school.classboard.domain.Homework;
import com.school.classboard.service.HomeworkService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 今日作业：展示当前班级今天及以后需要完成的作业。
 */
@Route(value = "homework", layout = MainLayout.class)
@PageTitle("今日作业")
@AnonymousAllowed
public class HomeworkView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final HomeworkService homeworkService;
    private final Long classId;

    public HomeworkView(HomeworkService homeworkService) {
        this.homeworkService = homeworkService;
        this.classId = ClassSession.getClassId();

        VerticalLayout root = getContent();
        root.setPadding(true);
        root.setSpacing(true);
        root.getStyle().set("max-width", "900px");

        H2 title = new H2("今日作业");
        title.getStyle().set("margin", "0");
        root.add(title);

        if (classId == null) {
            return;
        }

        List<Homework> homework = homeworkService.upcoming(classId);
        if (homework.isEmpty()) {
            root.add(new Span("最近没有作业安排，保持好状态。"));
            return;
        }

        Map<LocalDate, List<Homework>> byDate = new LinkedHashMap<>();
        for (Homework item : homework) {
            byDate.computeIfAbsent(item.getDueDate(), key -> new java.util.ArrayList<>()).add(item);
        }
        byDate.forEach((date, items) -> {
            H3 dateTitle = new H3(date.equals(LocalDate.now())
                    ? "今天（" + date.format(FMT) + "）"
                    : date.format(FMT) + " 截止");
            dateTitle.getStyle().set("margin", "0.5rem 0 0").set("font-size", "1.1rem");
            root.add(dateTitle);
            items.forEach(item -> root.add(card(item)));
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (ClassSession.getClassId() == null) {
            event.rerouteTo(LoginView.class);
        }
    }

    private VerticalLayout card(Homework homework) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 14px rgba(15,23,42,0.08)")
                .set("width", "100%");

        Span subject = new Span(homework.getSubject());
        subject.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "600")
                .set("padding", "0.15rem 0.55rem")
                .set("border-radius", "999px")
                .set("background", "#dbeafe")
                .set("color", "#1d4ed8");

        Span teacher = new Span(homework.getTeacher() == null ? "" : " · " + homework.getTeacher());
        teacher.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(new Span(homework.getContent() == null ? "" : homework.getContent()));
        content.getStyle().set("font-size", "1rem").set("line-height", "1.7").set("margin-top", "0.4rem");

        card.add(subject, teacher, content);
        return card;
    }
}
