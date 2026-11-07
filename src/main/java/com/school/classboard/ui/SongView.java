package com.school.classboard.ui;

import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import com.school.classboard.service.SongService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 广播室点歌页：学生提交点歌申请，广播室审核后进入歌单。
 */
@Route(value = "songs", layout = MainLayout.class)
@PageTitle("广播点歌")
@AnonymousAllowed
public class SongView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SongService songService;
    private final Long classId;

    private final TextField song = new TextField("歌曲名");
    private final TextField artist = new TextField("歌手");
    private final TextField requester = new TextField("点歌人");
    private final TextArea reason = new TextArea("想说的话");
    private final VerticalLayout notices = new VerticalLayout();
    private final VerticalLayout records = new VerticalLayout();
    private final VerticalLayout playlist = new VerticalLayout();
    private final H3 recordTitle = new H3("本班点歌记录");

    public SongView(SongService songService) {
        this.songService = songService;
        this.classId = ClassSession.getClassId();

        VerticalLayout root = getContent();
        root.setPadding(true);
        root.setSpacing(true);
        root.getStyle().set("max-width", "1000px");

        H2 title = new H2("广播点歌");
        title.getStyle().set("margin", "0");

        Span hint = new Span("每天午休与傍晚广播时段播放，提交后由广播室审核。");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)");

        song.setPlaceholder("如：夜空中最亮的星");
        artist.setPlaceholder("如：逃跑计划");
        requester.setPlaceholder("姓名或昵称");
        reason.setPlaceholder("想对谁说的话、点歌理由（可选）");
        reason.setHeight("90px");

        Button submit = new Button("提交点歌", e -> submit());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout form = new VerticalLayout(song, artist, requester, reason, submit);
        form.setPadding(true);
        form.setSpacing(true);
        form.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 14px rgba(15,23,42,0.08)")
                .set("max-width", "560px");

        H3 listTitle = new H3("今日歌单");
        listTitle.getStyle().set("margin", "1rem 0 0");
        playlist.setPadding(false);
        playlist.setSpacing(true);

        notices.setPadding(false);
        notices.setSpacing(true);
        notices.setVisible(false);

        recordTitle.getStyle().set("margin", "1rem 0 0");
        Button refreshButton = new Button("刷新", e -> refresh());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout recordHeader = new HorizontalLayout(recordTitle, refreshButton);
        recordHeader.setWidthFull();
        recordHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        recordHeader.expand(recordTitle);

        records.setPadding(false);
        records.setSpacing(true);

        root.add(title, hint, notices, form, recordHeader, records, listTitle, playlist);
        refresh();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (ClassSession.getClassId() == null) {
            event.rerouteTo(LoginView.class);
        }
    }

    private void submit() {
        if (song.isEmpty()) {
            song.setInvalid(true);
            song.setErrorMessage("请填写歌曲名");
            return;
        }
        song.setInvalid(false);
        songService.submit(classId, song.getValue(), artist.getValue(), requester.getValue(), reason.getValue());
        song.clear();
        artist.clear();
        reason.clear();
        Notification.show("点歌申请已提交，等待广播室审核");
        refresh();
    }

    private void refresh() {
        refreshNotices();
        refreshRecords();
        refreshPlaylist();
    }

    /** 被驳回的申请不删除，作为审核通知保留在点歌页顶部 */
    private void refreshNotices() {
        notices.removeAll();
        notices.setVisible(false);
        if (classId == null) {
            return;
        }

        List<SongRequest> rejected = songService.forClass(classId).stream()
                .filter(request -> request.getStatus() == SongStatus.REJECTED
                        && request.getRejectReadAt() == null)
                .limit(10)
                .toList();
        if (rejected.isEmpty()) {
            return;
        }

        H3 heading = new H3("审核通知");
        heading.getStyle().set("margin", "0.5rem 0 0").set("font-size", "1.05rem");
        notices.add(heading);
        rejected.forEach(request -> notices.add(noticeCard(request)));
        notices.setVisible(true);
    }

    private VerticalLayout noticeCard(SongRequest request) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle()
                .set("background", "#fef2f2")
                .set("border-left", "4px solid #dc2626")
                .set("border-radius", "10px")
                .set("padding", "0.7rem 0.9rem");

        Span title = new Span("《" + request.getSong() + "》未通过审核");
        title.getStyle().set("font-weight", "600").set("color", "#991b1b");

        Span reason = new Span("原因：" + (request.getRejectReason() == null || request.getRejectReason().isBlank()
                ? "广播室未填写具体原因" : request.getRejectReason()));
        reason.getStyle().set("font-size", "0.9rem").set("line-height", "1.6");

        Span meta = new Span("点歌人：" + requesterName(request) + " · " + reviewTime(request));
        meta.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)");

        Button confirm = new Button("已读确认", e -> {
            songService.markRejectRead(request.getId());
            Notification.show("已确认已读");
            refresh();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        confirm.getStyle().set("margin-top", "0.45rem");

        card.add(title, reason, meta, confirm);
        return card;
    }

    /** 本班全部点歌记录，驳回/待审核的申请也不会消失 */
    private void refreshRecords() {
        records.removeAll();
        if (classId == null) {
            return;
        }

        List<SongRequest> all = songService.forClass(classId);
        recordTitle.setText("本班点歌记录（" + all.size() + "）");
        if (all.isEmpty()) {
            records.add(new Span("本班还没有点歌记录。"));
            return;
        }
        all.stream().limit(10).forEach(request -> records.add(recordRow(request)));
    }

    private HorizontalLayout recordRow(SongRequest request) {
        VerticalLayout left = new VerticalLayout();
        left.setPadding(false);
        left.setSpacing(false);

        Span song = new Span(request.getSong());
        song.getStyle().set("font-weight", "600");

        Span artist = new Span(request.getArtist() == null ? "" : request.getArtist());
        artist.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        Span meta = new Span(requesterName(request) + " · "
                + (request.getCreatedAt() == null ? "" : request.getCreatedAt().format(FMT)));
        meta.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-tertiary-text-color)");

        left.add(song, artist, meta);

        if (request.getStatus() == SongStatus.REJECTED) {
            Span reason = new Span("驳回原因："
                    + (request.getRejectReason() == null || request.getRejectReason().isBlank()
                    ? "未填写" : request.getRejectReason()));
            reason.getStyle().set("font-size", "0.85rem").set("color", "#991b1b").set("margin-top", "0.2rem");
            left.add(reason);
        }

        VerticalLayout right = new VerticalLayout();
        right.setPadding(false);
        right.setSpacing(false);
        right.setAlignItems(FlexComponent.Alignment.END);
        right.add(statusBadge(request.getStatus()));

        if (request.getStatus() == SongStatus.REJECTED) {
            Span read = new Span(request.getRejectReadAt() == null ? "未读" : "已读");
            read.getStyle()
                    .set("font-size", "0.75rem")
                    .set("color", request.getRejectReadAt() == null
                            ? "#b91c1c" : "var(--lumo-tertiary-text-color)");
            right.add(read);
        }

        HorizontalLayout row = new HorizontalLayout(left, right);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.expand(left);
        row.getStyle()
                .set("background", request.getStatus() == SongStatus.REJECTED ? "#fff7f7" : "#ffffff")
                .set("border-left", "4px solid " + statusColor(request.getStatus()))
                .set("border-radius", "10px")
                .set("padding", "0.6rem 1rem")
                .set("box-shadow", "0 2px 8px rgba(15,23,42,0.06)");
        return row;
    }

    private void refreshPlaylist() {
        playlist.removeAll();
        List<SongRequest> requests = songService.playlist();
        if (requests.isEmpty()) {
            playlist.add(new Span("今天还没有排歌，快来点一首吧。"));
            return;
        }
        requests.forEach(request -> playlist.add(row(request)));
    }

    private HorizontalLayout row(SongRequest request) {
        Span name = new Span(request.getSong());
        name.getStyle().set("font-size", "1.1rem").set("font-weight", "600");

        Span artist = new Span(request.getArtist() == null ? "" : "- " + request.getArtist());
        artist.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span meta = new Span(requesterName(request)
                + (request.getReason() == null || request.getReason().isBlank() ? "" : "：" + request.getReason()));
        meta.getStyle().set("font-size", "0.9rem").set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout left = new VerticalLayout(name, artist, meta);
        left.setPadding(false);
        left.setSpacing(false);

        Span time = new Span(request.getCreatedAt() == null ? "" : request.getCreatedAt().format(FMT));
        time.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-tertiary-text-color)");

        HorizontalLayout right = new HorizontalLayout(statusBadge(request.getStatus()), time);
        right.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout row = new HorizontalLayout(left, right);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.expand(left);
        row.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "10px")
                .set("padding", "0.6rem 1rem")
                .set("box-shadow", "0 2px 8px rgba(15,23,42,0.06)");
        return row;
    }

    private String requesterName(SongRequest request) {
        return request.getRequester() == null || request.getRequester().isBlank()
                ? "匿名" : request.getRequester();
    }

    private String reviewTime(SongRequest request) {
        if (request.getReviewedAt() != null) {
            return request.getReviewedAt().format(FMT);
        }
        return request.getCreatedAt() == null ? "" : request.getCreatedAt().format(FMT);
    }

    private Span statusBadge(SongStatus status) {
        Span badge = new Span(status.getLabel());
        badge.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.15rem 0.6rem")
                .set("border-radius", "999px")
                .set("white-space", "nowrap")
                .set("background", switch (status) {
                    case PENDING -> "#fef3c7";
                    case APPROVED -> "#dbeafe";
                    case PLAYED -> "#dcfce7";
                    case REJECTED -> "#fee2e2";
                })
                .set("color", switch (status) {
                    case PENDING -> "#92400e";
                    case APPROVED -> "#1d4ed8";
                    case PLAYED -> "#166534";
                    case REJECTED -> "#991b1b";
                });
        return badge;
    }

    private String statusColor(SongStatus status) {
        return switch (status) {
            case PENDING -> "#f59e0b";
            case APPROVED -> "#2563eb";
            case PLAYED -> "#16a34a";
            case REJECTED -> "#dc2626";
        };
    }
}
