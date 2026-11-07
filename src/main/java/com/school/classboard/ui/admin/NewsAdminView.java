package com.school.classboard.ui.admin;

import com.school.classboard.domain.NewsItem;
import com.school.classboard.service.ImageStorageService;
import com.school.classboard.service.NewsService;
import com.school.classboard.service.NewsSyncService;
import com.school.classboard.ui.ImageField;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 后台：精选新闻维护。
 */
@Route(value = "admin/news", layout = MainLayout.class)
@PageTitle("新闻管理")
@PermitAll
public class NewsAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NewsService service;
    private final NewsSyncService syncService;
    private final ImageStorageService imageStorage;
    private final Grid<NewsItem> grid = new Grid<>(NewsItem.class, false);

    public NewsAdminView(NewsService service, NewsSyncService syncService, ImageStorageService imageStorage) {
        this.service = service;
        this.syncService = syncService;
        this.imageStorage = imageStorage;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("新闻管理");
        title.getStyle().set("margin", "0");

        Button sync = new Button("同步教育新闻", e -> {
            NewsSyncService.SyncResult result = syncService.sync();
            Notification.show("同步完成：新增 " + result.added() + " 条，更新 " + result.updated() + " 条");
            refresh();
        });
        sync.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button add = new Button("新增新闻", e -> openEditor(new NewsItem()));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(sync, add);
        toolbar.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(title, toolbar);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        Span hint = new Span("「同步新华网 / 人民网 / 中国教育新闻网」会拉取最新教育新闻；"
                + "同步后可编辑、配图或下线，内容默认展示在新闻页与班牌。");
        hint.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");
        root.add(hint);

        grid.addColumn(NewsItem::getTitle).setHeader("标题").setAutoWidth(true);
        grid.addColumn(item -> text(item.getCategory())).setHeader("分类").setAutoWidth(true);
        grid.addColumn(item -> text(item.getSource())).setHeader("来源").setAutoWidth(true);
        grid.addColumn(item -> item.getImageUrl() == null || item.getImageUrl().isBlank() ? "—" : "有")
                .setHeader("配图").setAutoWidth(true);
        grid.addColumn(item -> item.isFeatured() ? "是" : "否").setHeader("推荐").setAutoWidth(true);
        grid.addComponentColumn(this::statusBadge).setHeader("状态").setAutoWidth(true);
        grid.addColumn(item -> item.getPublishedAt() == null ? "" : item.getPublishedAt().format(FMT))
                .setHeader("发布时间").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("操作").setAutoWidth(true);
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);

        refresh();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private Component statusBadge(NewsItem item) {
        Span span = new Span(item.isActive() ? "展示中" : "已下线");
        span.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.15rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", item.isActive() ? "#dcfce7" : "#f1f5f9")
                .set("color", item.isActive() ? "#166534" : "#475569");
        return span;
    }

    private Component actions(NewsItem item) {
        Button edit = new Button("编辑", e -> openEditor(item));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Button delete = new Button("删除", e -> {
            service.delete(item.getId());
            refresh();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    private void refresh() {
        grid.setItems(service.findAll());
    }

    private void openEditor(NewsItem item) {
        boolean isNew = item.getId() == null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "新增新闻" : "编辑新闻");
        dialog.setWidth("560px");

        TextField title = new TextField("标题");
        title.setWidthFull();
        ComboBox<String> category = new ComboBox<>("分类");
        category.setItems("校园", "科技", "时政", "体育", "文化");
        category.setAllowCustomValue(true);
        TextField source = new TextField("来源");
        source.setPlaceholder("如：校园网、人民日报");
        TextField url = new TextField("原文链接");
        url.setPlaceholder("可选");
        url.setWidthFull();
        TextArea summary = new TextArea("摘要");
        summary.setHeight("120px");
        summary.setWidthFull();
        ImageField image = new ImageField(imageStorage);
        image.setValue(item.getImageUrl());
        Checkbox featured = new Checkbox("推荐到班牌首页");
        Checkbox active = new Checkbox("展示中");

        VerticalLayout form = new VerticalLayout(title, category, source, url, summary, image, featured, active);
        form.setPadding(false);
        form.setWidthFull();

        Binder<NewsItem> binder = new Binder<>(NewsItem.class);
        binder.forField(title).asRequired("请填写标题").bind(NewsItem::getTitle, NewsItem::setTitle);
        binder.forField(category).bind(NewsItem::getCategory, NewsItem::setCategory);
        binder.forField(source).bind(NewsItem::getSource, NewsItem::setSource);
        binder.forField(url).bind(NewsItem::getUrl, NewsItem::setUrl);
        binder.forField(summary).bind(NewsItem::getSummary, NewsItem::setSummary);
        binder.forField(featured).bind(NewsItem::isFeatured, NewsItem::setFeatured);
        binder.forField(active).bind(NewsItem::isActive, NewsItem::setActive);
        binder.setBean(item);
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (binder.validate().isOk()) {
                if (item.getPublishedAt() == null) {
                    item.setPublishedAt(LocalDateTime.now());
                }
                item.setImageUrl(image.getValue());
                service.save(item);
                refresh();
                dialog.close();
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);

        dialog.open();
    }
}
