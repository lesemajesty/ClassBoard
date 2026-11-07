package com.school.classboard.ui;

import com.school.classboard.domain.Announcement;
import com.school.classboard.service.AnnouncementService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 通知公告列表页：展示全校公告与当前班级公告。
 */
@Route(value = "announcements", layout = MainLayout.class)
@PageTitle("通知公告")
@AnonymousAllowed
public class AnnouncementView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnnouncementService announcementService;

    public AnnouncementView(AnnouncementService announcementService) {
        this.announcementService = announcementService;

        VerticalLayout root = getContent();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("通知公告");
        title.getStyle().set("margin", "0");
        root.add(title);

        Long classId = ClassSession.getClassId();
        if (classId == null) {
            // 未选择班级，由 beforeEnter 跳转到登录页
            return;
        }

        List<Announcement> list = announcementService.findForClass(classId);
        if (list.isEmpty()) {
            root.add(new Span("暂无公告"));
            return;
        }
        list.forEach(a -> root.add(card(a)));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (ClassSession.getClassId() == null) {
            event.rerouteTo(LoginView.class);
        }
    }

    private VerticalLayout card(Announcement a) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 14px rgba(15,23,42,0.08)")
                .set("max-width", "1000px")
                .set("width", "100%");

        HorizontalLayout head = new HorizontalLayout();
        head.setAlignItems(FlexComponent.Alignment.CENTER);
        head.setSpacing(true);

        H3 heading = new H3(a.getTitle());
        heading.getStyle().set("margin", "0").set("font-size", "1.15rem");
        head.add(heading);

        if (a.getCategory() != null) {
            Span badge = new Span(a.getCategory());
            badge.getStyle()
                    .set("background", "#e0e7ff")
                    .set("color", "#3730a3")
                    .set("font-size", "0.75rem")
                    .set("padding", "0.15rem 0.5rem")
                    .set("border-radius", "999px");
            head.add(badge);
        }
        if (a.isPinned()) {
            Span pinned = new Span("置顶");
            pinned.getStyle().set("color", "#dc2626").set("font-size", "0.8rem");
            head.add(pinned);
        }

        Span meta = new Span((a.getClassId() == null ? "全校　" : "本班　")
                + (a.getPublisher() == null ? "" : "发布人：" + a.getPublisher() + "　")
                + (a.getPublishedAt() == null ? "" : a.getPublishedAt().format(FMT))
                + (a.getEventDate() == null ? "" : "　相关日期：" + a.getEventDate().format(DATE_FMT)));
        meta.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        Span content = new Span(a.getContent() == null ? "" : a.getContent());
        content.getStyle().set("font-size", "1rem").set("line-height", "1.7").set("margin-top", "0.5rem");

        card.add(head, meta);

        if (a.getImageUrl() != null && !a.getImageUrl().isBlank()) {
            Image image = new Image(a.getImageUrl(), a.getTitle());
            image.setWidthFull();
            image.getStyle()
                    .set("max-height", "320px")
                    .set("object-fit", "cover")
                    .set("border-radius", "10px")
                    .set("margin-top", "0.5rem");
            card.add(image);
        }
        card.add(content);
        return card;
    }
}
