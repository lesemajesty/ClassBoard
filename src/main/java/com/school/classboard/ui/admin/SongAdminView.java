package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.SongService;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台：广播点歌审核。
 */
@Route(value = "admin/songs", layout = MainLayout.class)
@PageTitle("点歌审核")
@PermitAll
public class SongAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SongService service;
    private final ClassInfoService classInfoService;
    private final Grid<SongRequest> grid = new Grid<>(SongRequest.class, false);

    private Map<Long, ClassInfo> classById = Map.of();

    public SongAdminView(SongService service, ClassInfoService classInfoService) {
        this.service = service;
        this.classInfoService = classInfoService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("点歌审核");
        title.getStyle().set("margin", "0");

        Span hint = new Span("通过后进入班牌歌单；驳回时填写的原因会保留给学生查看；播放完记得标记为「已播放」。");
        hint.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        root.add(title, hint);

        grid.addColumn(SongRequest::getSong).setHeader("歌曲").setWidth("150px").setFlexGrow(0);
        grid.addColumn(r -> text(r.getArtist())).setHeader("歌手").setWidth("110px").setFlexGrow(0);
        grid.addColumn(r -> text(r.getRequester())).setHeader("点歌人").setWidth("100px").setFlexGrow(0);
        grid.addColumn(this::className).setHeader("班级").setWidth("100px").setFlexGrow(0);
        grid.addColumn(r -> text(r.getReason())).setHeader("想说的话").setWidth("160px").setFlexGrow(1);
        grid.addComponentColumn(this::statusCell).setHeader("状态").setWidth("100px").setFlexGrow(0);
        grid.addColumn(this::rejectReasonLabel).setHeader("驳回原因").setWidth("160px").setFlexGrow(0);
        grid.addColumn(r -> r.getCreatedAt() == null ? "" : r.getCreatedAt().format(FMT))
                .setHeader("提交时间").setWidth("130px").setFlexGrow(0);
        grid.addComponentColumn(this::actions).setHeader("操作").setWidth("190px").setFlexGrow(0);
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);

        refresh();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String className(SongRequest request) {
        ClassInfo clazz = request.getClassId() == null ? null : classById.get(request.getClassId());
        return clazz == null ? "—" : clazz.getName();
    }

    private String rejectReasonLabel(SongRequest request) {
        if (request.getStatus() != SongStatus.REJECTED) {
            return "—";
        }
        return request.getRejectReason() == null || request.getRejectReason().isBlank()
                ? "未填写" : request.getRejectReason();
    }

    private Component statusBadge(SongRequest request) {
        Span span = new Span(request.getStatus().getLabel());
        span.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.15rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", switch (request.getStatus()) {
                    case PENDING -> "#fef3c7";
                    case APPROVED -> "#dbeafe";
                    case PLAYED -> "#dcfce7";
                    case REJECTED -> "#fee2e2";
                })
                .set("color", switch (request.getStatus()) {
                    case PENDING -> "#92400e";
                    case APPROVED -> "#1d4ed8";
                    case PLAYED -> "#166534";
                    case REJECTED -> "#991b1b";
                });
        return span;
    }

    /** 已驳回的申请额外显示学生是否已读确认 */
    private Component statusCell(SongRequest request) {
        if (request.getStatus() != SongStatus.REJECTED) {
            return statusBadge(request);
        }

        VerticalLayout cell = new VerticalLayout();
        cell.setPadding(false);
        cell.setSpacing(false);

        Span read = new Span(request.getRejectReadAt() == null ? "未读" : "已读");
        read.getStyle()
                .set("font-size", "0.72rem")
                .set("color", request.getRejectReadAt() == null ? "#b91c1c" : "var(--lumo-tertiary-text-color)");

        cell.add(statusBadge(request), read);
        return cell;
    }

    private Component actions(SongRequest request) {
        Button approve = new Button("通过", e -> update(request, SongStatus.APPROVED));
        approve.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        approve.setEnabled(request.getStatus() == SongStatus.PENDING);

        Button played = new Button("已播放", e -> update(request, SongStatus.PLAYED));
        played.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
        played.setEnabled(request.getStatus() == SongStatus.APPROVED);

        Button reject = new Button("驳回", e -> openRejectDialog(request));
        reject.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        reject.setEnabled(request.getStatus() == SongStatus.PENDING);

        return new HorizontalLayout(approve, played, reject);
    }

    /** 驳回时填写原因，原因会随申请一起保留，学生可在点歌页看到通知 */
    private void openRejectDialog(SongRequest request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("驳回点歌");

        Span info = new Span("《" + request.getSong() + "》驳回后，点歌人会在「广播点歌」页的审核通知和本班记录中看到结果。");
        info.getStyle().set("font-size", "0.9rem").set("line-height", "1.6").set("max-width", "440px");

        TextArea reason = new TextArea("驳回原因");
        reason.setPlaceholder("会显示给点歌人，例如：版权原因 / 不适合午休广播（可留空）");
        reason.setWidthFull();
        reason.setHeight("100px");

        VerticalLayout content = new VerticalLayout(info, reason);
        content.setPadding(false);
        content.setWidth("480px");
        dialog.add(content);

        Button cancel = new Button("取消", e -> dialog.close());
        Button confirm = new Button("确认驳回", e -> {
            service.reject(request.getId(), reason.getValue());
            refresh();
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void update(SongRequest request, SongStatus status) {
        service.updateStatus(request.getId(), status);
        refresh();
    }

    private void refresh() {
        List<ClassInfo> classes = classInfoService.findAll();
        classById = classes.stream().collect(Collectors.toMap(ClassInfo::getId, Function.identity()));
        grid.setItems(service.findAll());
    }
}
