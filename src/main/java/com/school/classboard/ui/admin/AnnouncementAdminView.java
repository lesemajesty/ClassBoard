package com.school.classboard.ui.admin;

import com.school.classboard.domain.Announcement;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.service.AnnouncementService;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.ImageStorageService;
import com.school.classboard.ui.ImageField;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台：公告发布与管理（需要登录）。
 */
@Route(value = "admin/announcements", layout = MainLayout.class)
@PageTitle("公告发布")
@PermitAll
public class AnnouncementAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final AnnouncementService service;
    private final ClassInfoService classInfoService;
    private final ImageStorageService imageStorage;
    private final Grid<Announcement> grid = new Grid<>(Announcement.class, false);

    private Map<Long, ClassInfo> classById = Map.of();

    public AnnouncementAdminView(AnnouncementService service,
                                 ClassInfoService classInfoService,
                                 ImageStorageService imageStorage) {
        this.service = service;
        this.classInfoService = classInfoService;
        this.imageStorage = imageStorage;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("公告发布");
        title.getStyle().set("margin", "0");

        Button add = new Button("新增公告", e -> openEditor(new Announcement()));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, add);
        header.setWidthFull();
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        grid.addColumn(Announcement::getTitle).setHeader("标题").setAutoWidth(true);
        grid.addColumn(a -> a.getCategory() == null ? "" : a.getCategory()).setHeader("分类").setAutoWidth(true);
        grid.addColumn(this::scopeLabel).setHeader("范围").setAutoWidth(true);
        grid.addColumn(a -> a.getImageUrl() == null || a.getImageUrl().isBlank() ? "—" : "有")
                .setHeader("配图").setAutoWidth(true);
        grid.addColumn(a -> a.isPinned() ? "是" : "否").setHeader("置顶").setAutoWidth(true);
        grid.addColumn(a -> a.getPublisher() == null ? "" : a.getPublisher()).setHeader("发布人").setAutoWidth(true);
        grid.addColumn(a -> a.getPublishedAt() == null ? "" : a.getPublishedAt().format(FMT))
                .setHeader("发布时间").setAutoWidth(true);
        grid.addComponentColumn(this::dateCell).setHeader("相关日期 / 有效期").setAutoWidth(true);
        grid.addComponentColumn(this::statusBadge).setHeader("状态").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("操作").setAutoWidth(true);
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);

        refresh();
    }

    private Component statusBadge(Announcement a) {
        boolean expired = a.getExpireAt() != null && !a.getExpireAt().isAfter(LocalDateTime.now());
        String label = expired ? "已过期" : a.isActive() ? "已发布" : "已下线";
        Span span = new Span(label);
        span.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.15rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", expired ? "#f1f5f9" : a.isActive() ? "#dcfce7" : "#fee2e2")
                .set("color", expired ? "#475569" : a.isActive() ? "#166534" : "#991b1b");
        return span;
    }

    /** 相关日期用于班级生活倒计时，有效期到点后自动下线 */
    private Component dateCell(Announcement a) {
        VerticalLayout cell = new VerticalLayout();
        cell.setPadding(false);
        cell.setSpacing(false);
        cell.getStyle().set("font-size", "0.8rem").set("line-height", "1.5");
        if (a.getEventDate() != null) {
            cell.add(new Span("活动：" + a.getEventDate().format(DATE_FMT)));
        }
        if (a.getExpireAt() != null) {
            cell.add(new Span("有效至：" + a.getExpireAt().format(FMT)));
        }
        if (a.getEventDate() == null && a.getExpireAt() == null) {
            cell.add(new Span("—"));
        }
        return cell;
    }

    private Component actions(Announcement a) {
        Button edit = new Button("编辑", e -> openEditor(a));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Button delete = new Button("删除", e -> {
            service.delete(a.getId());
            refresh();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    private void refresh() {
        List<ClassInfo> classes = classInfoService.findAll();
        classById = classes.stream().collect(Collectors.toMap(ClassInfo::getId, Function.identity()));
        grid.setItems(service.findAll());
    }

    /** 公告适用范围：全校或某个班级 */
    private String scopeLabel(Announcement announcement) {
        if (announcement.getClassId() == null) {
            return "全校";
        }
        ClassInfo clazz = classById.get(announcement.getClassId());
        return clazz == null ? "已删除的班级" : clazz.getName();
    }

    private void openEditor(Announcement announcement) {
        boolean isNew = announcement.getId() == null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "新增公告" : "编辑公告");

        TextField title = new TextField("标题");
        TextArea content = new TextArea("正文");
        content.setHeight("160px");
        ComboBox<String> category = new ComboBox<>("分类");
        category.setItems("通知", "活动", "荣誉", "紧急");
        ComboBox<ClassInfo> scope = new ComboBox<>("适用班级");
        scope.setItems(classInfoService.findAll());
        scope.setItemLabelGenerator(ClassInfo::getName);
        scope.setPlaceholder("留空表示面向全校");
        scope.setClearButtonVisible(true);
        scope.setWidthFull();
        TextField publisher = new TextField("发布人");
        DatePicker eventDate = new DatePicker("相关日期（考试 / 活动）");
        eventDate.setPlaceholder("用于班级生活倒计时，可留空");
        eventDate.setWidthFull();
        DatePicker expireDate = new DatePicker("有效期至");
        expireDate.setPlaceholder("当天 23:59 自动下线，留空长期有效");
        expireDate.setWidthFull();
        Checkbox pinned = new Checkbox("置顶");
        Checkbox active = new Checkbox("发布（取消勾选即下线）");
        ImageField image = new ImageField(imageStorage);
        image.setValue(announcement.getImageUrl());

        Span dateHint = new Span("「相关日期」会同步到班牌班级生活的倒计时；「有效期至」到点后公告自动下线。");
        dateHint.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout form = new VerticalLayout(title, category, scope, publisher, eventDate, expireDate,
                dateHint, content, image, pinned, active);
        form.setPadding(false);
        form.setWidth("480px");

        Binder<Announcement> binder = new Binder<>(Announcement.class);
        binder.forField(title).asRequired("请填写标题").bind(Announcement::getTitle, Announcement::setTitle);
        binder.forField(category).bind(Announcement::getCategory, Announcement::setCategory);
        binder.forField(publisher).bind(Announcement::getPublisher, Announcement::setPublisher);
        binder.forField(eventDate).bind(Announcement::getEventDate, Announcement::setEventDate);
        binder.forField(content).bind(Announcement::getContent, Announcement::setContent);
        binder.forField(pinned).bind(Announcement::isPinned, Announcement::setPinned);
        binder.forField(active).bind(Announcement::isActive, Announcement::setActive);

        binder.setBean(announcement);
        eventDate.setValue(announcement.getEventDate());
        expireDate.setValue(announcement.getExpireAt() == null ? null : announcement.getExpireAt().toLocalDate());
        if (isNew && expireDate.getValue() == null) {
            expireDate.setValue(LocalDate.now().plusDays(30));
        }
        eventDate.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                expireDate.setValue(event.getValue().plusDays(1));
            }
        });
        scope.setValue(announcement.getClassId() == null ? null
                : classInfoService.findById(announcement.getClassId()).orElse(null));
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (binder.validate().isOk()) {
                ClassInfo selected = scope.getValue();
                announcement.setClassId(selected == null ? null : selected.getId());
                announcement.setImageUrl(image.getValue());
                LocalDate expiry = expireDate.getValue();
                announcement.setExpireAt(expiry == null ? null : expiry.atTime(23, 59, 59));
                service.save(announcement);
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
