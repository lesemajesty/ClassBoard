package com.school.classboard.ui;

import com.school.classboard.domain.NewsItem;
import com.school.classboard.service.NewsService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 精选新闻列表页：校园与国内外要闻摘要。
 */
@Route(value = "news", layout = MainLayout.class)
@PageTitle("精选新闻")
@AnonymousAllowed
public class NewsView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    public NewsView(NewsService newsService) {
        VerticalLayout root = getContent();
        root.setPadding(true);
        root.setSpacing(true);
        root.getStyle().set("max-width", "1000px");

        H2 title = new H2("精选新闻");
        title.getStyle().set("margin", "0");
        root.add(title);

        List<NewsItem> items = newsService.latest(20);
        if (items.isEmpty()) {
            root.add(new Span("暂无新闻，请管理员在后台维护。"));
            return;
        }
        items.forEach(item -> root.add(card(item)));
    }

    private VerticalLayout card(NewsItem item) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 14px rgba(15,23,42,0.08)")
                .set("width", "100%");

        H3 heading = new H3(item.getTitle());
        heading.getStyle().set("margin", "0 0 0.35rem 0").set("font-size", "1.2rem");

        Span meta = new Span((item.getCategory() == null ? "综合" : item.getCategory())
                + "　" + (item.getSource() == null ? "" : item.getSource())
                + "　" + (item.getPublishedAt() == null ? "" : item.getPublishedAt().format(FMT)));
        meta.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        Span summary = new Span(item.getSummary() == null ? "" : item.getSummary());
        summary.getStyle().set("font-size", "1rem").set("line-height", "1.7").set("margin-top", "0.5rem");

        card.add(heading, meta, summary);

        if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
            Image image = new Image(item.getImageUrl(), item.getTitle());
            image.setWidthFull();
            image.getStyle()
                    .set("max-height", "200px")
                    .set("object-fit", "cover")
                    .set("border-radius", "10px")
                    .set("margin-top", "0.5rem");
            card.add(image);
        }

        Button detail = new Button("查看详情", e -> UI.getCurrent().navigate("news/" + item.getId()));
        detail.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        detail.getStyle().set("margin-top", "0.5rem");
        card.add(detail);

        if (item.getUrl() != null && !item.getUrl().isBlank()) {
            Anchor link = new Anchor(item.getUrl(), "查看原文");
            link.setTarget("_blank");
            link.getStyle().set("margin-top", "0.5rem").set("font-size", "0.9rem");
            card.add(link);
        }
        return card;
    }
}
