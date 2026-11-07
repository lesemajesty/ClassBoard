package com.school.classboard.ui;

import com.school.classboard.domain.NewsItem;
import com.school.classboard.service.NewsService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.format.DateTimeFormatter;

/**
 * 新闻详情页：点击新闻列表中的「查看详情」进入，地址形如 /news/3。
 */
@Route(value = "news/:id", layout = MainLayout.class)
@PageTitle("新闻详情")
@AnonymousAllowed
public class NewsDetailView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NewsService newsService;

    public NewsDetailView(NewsService newsService) {
        this.newsService = newsService;

        VerticalLayout root = getContent();
        root.setPadding(true);
        root.setSpacing(true);
        root.getStyle().set("max-width", "900px");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        render(event.getRouteParameters().get("id").orElse(null));
    }

    private void render(String parameter) {
        VerticalLayout root = getContent();
        root.removeAll();

        Button back = new Button("返回新闻列表", e -> UI.getCurrent().navigate(NewsView.class));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        NewsItem item = null;
        try {
            item = newsService.findById(Long.valueOf(parameter)).orElse(null);
        } catch (NumberFormatException | NullPointerException ignored) {
            // 参数非法时按“新闻不存在”处理
        }

        if (item == null) {
            H2 missing = new H2("新闻不存在或已下线");
            missing.getStyle().set("margin", "0");
            root.add(back, missing);
            return;
        }

        H2 title = new H2(item.getTitle());
        title.getStyle().set("margin", "0");

        Span meta = new Span((item.getCategory() == null ? "综合" : item.getCategory())
                + "　" + (item.getSource() == null ? "" : item.getSource())
                + "　" + (item.getPublishedAt() == null ? "" : item.getPublishedAt().format(FMT)));
        meta.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.9rem");

        Span summary = new Span(item.getSummary() == null ? "" : item.getSummary());
        summary.getStyle()
                .set("font-size", "1.05rem")
                .set("line-height", "1.9")
                .set("margin-top", "0.75rem")
                .set("white-space", "pre-line");

        VerticalLayout card = new VerticalLayout(title, meta);
        card.setPadding(true);
        card.setSpacing(true);

        if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
            Image image = new Image(item.getImageUrl(), item.getTitle());
            image.setWidthFull();
            image.getStyle()
                    .set("max-height", "360px")
                    .set("object-fit", "cover")
                    .set("border-radius", "10px");
            card.add(image);
        }
        card.add(summary);
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "14px")
                .set("box-shadow", "0 6px 18px rgba(15,23,42,0.08)")
                .set("width", "100%");

        root.add(back, card);

        if (item.getUrl() != null && !item.getUrl().isBlank()) {
            Anchor link = new Anchor(item.getUrl(), "查看原文");
            link.setTarget("_blank");
            link.getStyle().set("font-size", "0.9rem");
            root.add(link);
        }
    }
}
