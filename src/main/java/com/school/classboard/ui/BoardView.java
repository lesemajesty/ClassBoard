package com.school.classboard.ui;

import com.school.classboard.domain.Announcement;
import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Course;
import com.school.classboard.domain.DutyRoster;
import com.school.classboard.domain.Homework;
import com.school.classboard.domain.NewsItem;
import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import com.school.classboard.domain.WeatherForecast;
import com.school.classboard.service.AnnouncementService;
import com.school.classboard.service.CanteenService;
import com.school.classboard.service.CheckInService;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.CourseService;
import com.school.classboard.service.CountdownService;
import com.school.classboard.service.DutyService;
import com.school.classboard.service.HomeworkService;
import com.school.classboard.service.NewsService;
import com.school.classboard.service.SongService;
import com.school.classboard.service.StudentService;
import com.school.classboard.service.WeatherService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班牌首页：教室大屏的核心页面。
 *
 * <p>包含班级信息、通知公告、天气、签到统计、今日课表、食堂菜单、精选新闻、
 * 今日歌单以及倒计时 / 值日 / 生日祝福。页面每 30 秒轮询一次，无需手动刷新。
 */
@Route(value = "board", layout = MainLayout.class)
@PageTitle("电子班牌")
@AnonymousAllowed
public class BoardView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final int MAX_ANNOUNCEMENTS = 6;
    private static final int REFRESH_INTERVAL_MS = 30_000;
    /**
     * 离开大屏时把轮询间隔调大，而不是设为 0。
     * Vaadin 24.5 客户端 Poller 对 0 会调用 scheduleRepeating(0)，
     * 抛出 "must be positive"，导致后续侧边栏导航直接中断（页面空白）。
     */
    private static final int POLL_PAUSED_INTERVAL_MS = 3_600_000;

    private final ClassInfoService classInfoService;
    private final AnnouncementService announcementService;
    private final CourseService courseService;
    private final HomeworkService homeworkService;
    private final WeatherService weatherService;
    private final CheckInService checkInService;
    private final CanteenService canteenService;
    private final NewsService newsService;
    private final SongService songService;
    private final CountdownService countdownService;
    private final DutyService dutyService;
    private final StudentService studentService;

    private Registration pollRegistration;

    public BoardView(ClassInfoService classInfoService,
                     AnnouncementService announcementService,
                     CourseService courseService,
                     HomeworkService homeworkService,
                     WeatherService weatherService,
                     CheckInService checkInService,
                     CanteenService canteenService,
                     NewsService newsService,
                     SongService songService,
                     CountdownService countdownService,
                     DutyService dutyService,
                     StudentService studentService) {
        this.classInfoService = classInfoService;
        this.announcementService = announcementService;
        this.courseService = courseService;
        this.homeworkService = homeworkService;
        this.weatherService = weatherService;
        this.checkInService = checkInService;
        this.canteenService = canteenService;
        this.newsService = newsService;
        this.songService = songService;
        this.countdownService = countdownService;
        this.dutyService = dutyService;
        this.studentService = studentService;

        // 卡片变为双倍宽度后高度会超过一屏，页面允许纵向滚动
        setWidthFull();
        setHeight("auto");
        setPadding(true);
        setSpacing(true);

        rebuild();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (currentClass() == null) {
            event.rerouteTo(LoginView.class);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.setPollInterval(REFRESH_INTERVAL_MS);
        pollRegistration = ui.addPollListener(event -> refreshFromServer(ui));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // 注意：不能设为 0，见 POLL_PAUSED_INTERVAL_MS 的说明
        detachEvent.getUI().setPollInterval(POLL_PAUSED_INTERVAL_MS);
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    private void refreshFromServer(UI ui) {
        if (!ui.isAttached()) {
            return;
        }
        if (currentClass() == null) {
            ClassSession.setClassId(null);
            ui.navigate(LoginView.class);
            return;
        }
        rebuild();
    }

    private void rebuild() {
        removeAll();
        ClassInfo clazz = currentClass();
        if (clazz == null) {
            return;
        }

        add(buildBanner(clazz));

        Div grid = new Div();
        grid.addClassName("cb-module-grid");
        grid.setWidthFull();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, minmax(0, 1fr))")
                .set("grid-auto-rows", "clamp(250px, calc(50vh - 120px), 440px)")
                .set("gap", "1rem")
                .set("width", "100%")
                .set("flex-shrink", "0");

        grid.add(panel("通知公告", announcementContent(clazz), this::openAnnouncements),
                weatherPanel(),
                checkInPanel(clazz),
                coursePanel(clazz),
                canteenPanel(),
                newsPanel(),
                songPanel(clazz),
                lifePanel(clazz));

        add(grid);
    }

    /** 当前终端绑定的班级；测试中可由子类覆盖，便于在没有会话时渲染整页 */
    protected ClassInfo currentClass() {
        Long classId = ClassSession.getClassId();
        return classId == null ? null : classInfoService.findById(classId).orElse(null);
    }

    // ==================== 顶部班级横幅 ====================

    private Component buildBanner(ClassInfo clazz) {
        HorizontalLayout banner = new HorizontalLayout();
        banner.addClassName("cb-banner");
        banner.setWidthFull();
        banner.setAlignItems(FlexComponent.Alignment.CENTER);
        banner.getStyle()
                .set("background", "linear-gradient(120deg, #1e3a8a 0%, #2563eb 55%, #0ea5e9 100%)")
                .set("border-radius", "18px")
                .set("padding", "1.5rem 2rem")
                .set("box-shadow", "0 12px 28px rgba(30,58,138,0.28)");

        VerticalLayout left = new VerticalLayout();
        left.addClassName("cb-banner-left");
        left.setSpacing(false);
        left.setPadding(false);

        H2 name = new H2(clazz.getName());
        name.getStyle().set("margin", "0").set("color", "#ffffff").set("font-size", "2.2rem").set("letter-spacing", "3px");
        left.add(name);

        Span motto = new Span(clazz.getMotto() == null ? "" : "“" + clazz.getMotto() + "”");
        motto.getStyle().set("color", "rgba(255,255,255,0.85)").set("font-size", "1rem");
        left.add(motto);

        HorizontalLayout right = new HorizontalLayout();
        right.addClassName("cb-banner-right");
        right.setSpacing(true);
        right.setAlignItems(FlexComponent.Alignment.CENTER);
        right.getStyle().set("flex-wrap", "wrap");
        right.add(infoChip("班主任", clazz.getHeadTeacher()));
        right.add(infoChip("教室", clazz.getClassroom()));
        right.add(infoChip("人数", clazz.getStudentCount() == null ? "-" : clazz.getStudentCount() + " 人"));

        banner.add(left, right);
        banner.setFlexGrow(1, left);
        return banner;
    }

    private Span infoChip(String label, String value) {
        Span chip = new Span(label + "：" + (value == null ? "-" : value));
        chip.getStyle()
                .set("background", "rgba(255,255,255,0.18)")
                .set("color", "#ffffff")
                .set("padding", "0.45rem 0.85rem")
                .set("border-radius", "999px")
                .set("font-size", "0.9rem")
                .set("white-space", "nowrap");
        return chip;
    }

    // ==================== 通知公告 ====================

    private Component announcementContent(ClassInfo clazz) {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);
        content.getStyle().set("overflow-y", "auto");

        List<Announcement> list = announcementService.findForClass(clazz.getId())
                .stream().limit(MAX_ANNOUNCEMENTS).toList();
        if (list.isEmpty()) {
            content.add(new Span("暂无公告"));
        } else {
            for (Announcement a : list) {
                content.add(announcementRow(a));
            }
        }
        return content;
    }

    private Component announcementRow(Announcement a) {
        VerticalLayout row = new VerticalLayout();
        row.setSpacing(false);
        row.setPadding(false);
        row.getStyle()
                .set("border-left", "4px solid " + categoryColor(a.getCategory()))
                .set("padding", "0.5rem 0.8rem")
                .set("background", "#f8fafc")
                .set("border-radius", "8px");

        HorizontalLayout head = new HorizontalLayout();
        head.setAlignItems(FlexComponent.Alignment.CENTER);
        head.setSpacing(true);

        if (a.getImageUrl() != null && !a.getImageUrl().isBlank()) {
            Image thumb = new Image(a.getImageUrl(), a.getTitle());
            thumb.getStyle()
                    .set("width", "40px")
                    .set("height", "40px")
                    .set("object-fit", "cover")
                    .set("border-radius", "6px")
                    .set("flex-shrink", "0");
            head.addComponentAsFirst(thumb);
        }

        Span badge = new Span(a.getCategory() == null ? "通知" : a.getCategory());
        badge.getStyle()
                .set("background", categoryColor(a.getCategory()))
                .set("color", "#ffffff")
                .set("font-size", "0.72rem")
                .set("padding", "0.12rem 0.45rem")
                .set("border-radius", "999px")
                .set("white-space", "nowrap")
                .set("flex-shrink", "0");
        head.add(badge);

        Span title = new Span(a.getTitle());
        title.getStyle().set("font-size", "1rem").set("font-weight", "600").set("min-width", "0");
        head.add(title);

        if (a.isPinned()) {
            Span pinned = new Span("置顶");
            pinned.getStyle()
                    .set("color", "#dc2626")
                    .set("font-size", "0.75rem")
                    .set("white-space", "nowrap")
                    .set("flex-shrink", "0");
            head.add(pinned);
        }

        Div meta = new Div();
        meta.setText((a.getPublisher() == null ? "" : a.getPublisher() + " · ")
                + (a.getPublishedAt() == null ? "" : a.getPublishedAt().format(TIME_FMT))
                + (a.getEventDate() == null ? "" : " · 相关日期 "
                + a.getEventDate().format(DateTimeFormatter.ofPattern("MM-dd"))));
        meta.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)");

        row.add(head, meta);
        return row;
    }

    private String categoryColor(String category) {
        if (category == null) {
            return "#2563eb";
        }
        return switch (category) {
            case "紧急" -> "#dc2626";
            case "活动" -> "#0ea5e9";
            case "荣誉" -> "#f59e0b";
            default -> "#2563eb";
        };
    }

    // ==================== 天气 ====================

    private Component weatherPanel() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        WeatherForecast today = weatherService.today().orElse(null);
        if (today == null) {
            content.add(new Span("天气数据准备中…"));
            return panel("天气", content, this::openWeather);
        }

        HorizontalLayout main = new HorizontalLayout();
        main.setAlignItems(FlexComponent.Alignment.CENTER);
        main.setSpacing(true);

        Span condition = new Span(today.getCondition());
        condition.getStyle().set("font-size", "1.8rem").set("font-weight", "700").set("color", "#1d4ed8");

        Span temperature = new Span(temp(today.getTempLow()) + " ~ " + temp(today.getTempHigh()) + "℃");
        temperature.getStyle().set("font-size", "1.3rem").set("font-weight", "600");

        main.add(condition, temperature);
        content.add(main);

        Span tip = new Span(today.getTip() == null ? "" : today.getTip());
        tip.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");
        content.add(tip);

        weatherService.tomorrow().ifPresent(tomorrow -> {
            Span next = new Span("明天 " + tomorrow.getCondition() + " "
                    + temp(tomorrow.getTempLow()) + "~" + temp(tomorrow.getTempHigh()) + "℃");
            next.getStyle().set("margin-top", "0.5rem").set("font-size", "0.88rem");
            content.add(next);
        });

        return panel("天气 · " + weatherService.getCity(), content, this::openWeather);
    }

    private String temp(Integer value) {
        return value == null ? "--" : String.valueOf(value);
    }

    // ==================== 签到统计 ====================

    private Component checkInPanel(ClassInfo clazz) {
        CheckInService.Stats stats = checkInService.stats(clazz.getId());

        Div numbers = new Div(
                statBlock("实到", stats.present() + "/" + stats.expected(), "#1d4ed8"),
                statBlock("迟到", String.valueOf(stats.late()), "#dc2626"),
                statBlock("请假", String.valueOf(stats.leave()), "#b45309"),
                statBlock("已签退", String.valueOf(stats.left()), "#475569"));
        numbers.setWidthFull();
        numbers.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, minmax(0, 1fr))")
                .set("gap", "0.4rem");

        VerticalLayout content = new VerticalLayout(numbers);
        content.setPadding(false);
        content.setSpacing(false);

        Span hint = new Span("到班后请在门口班牌刷卡签到");
        hint.getStyle().set("margin-top", "0.4rem").set("font-size", "0.8rem")
                .set("color", "var(--lumo-secondary-text-color)");
        content.add(hint);

        return panel("今日签到", content, () -> openCheckIn(clazz));
    }

    private VerticalLayout statBlock(String label, String value, String color) {
        Span number = new Span(value);
        number.getStyle().set("font-size", "1.5rem").set("font-weight", "700").set("color", color);

        Span caption = new Span(label);
        caption.getStyle().set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout block = new VerticalLayout(number, caption);
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle().set("min-width", "0");
        return block;
    }

    // ==================== 今日课表 ====================

    private Component coursePanel(ClassInfo clazz) {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.getStyle().set("overflow-y", "auto").set("gap", "0.35rem");

        int today = CourseService.currentDayOfWeek();
        content.add(new Span("今天 " + CourseService.dayName(today)));

        List<Course> todayCourses = courseService.findToday(clazz.getId());
        if (todayCourses.isEmpty()) {
            content.add(new Span("今天没有课程安排"));
        } else {
            todayCourses.stream().limit(6).forEach(course -> {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.setSpacing(true);
                row.getStyle()
                        .set("padding", "0.35rem 0.6rem")
                        .set("background", "#f1f5f9")
                        .set("border-radius", "8px");

                Span time = new Span(text(course.getStartTime()) + "-" + text(course.getEndTime()));
                time.getStyle().set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)")
                        .set("min-width", "92px");

                Span subject = new Span(text(course.getSubject()));
                subject.getStyle().set("font-size", "0.98rem").set("font-weight", "600");

                Span teacher = new Span(text(course.getTeacher()));
                teacher.getStyle().set("font-size", "0.82rem").set("color", "var(--lumo-secondary-text-color)");

                row.add(time, subject, teacher);
                row.setFlexGrow(1, subject);
                content.add(row);
            });
        }
        List<Homework> homework = homeworkService.forDate(clazz.getId(), LocalDate.now());
        if (!homework.isEmpty()) {
            String subjects = homework.stream()
                    .map(Homework::getSubject)
                    .distinct()
                    .collect(Collectors.joining("、"));
            Span summary = new Span("今日作业 " + homework.size() + " 项：" + subjects);
            summary.getStyle()
                    .set("margin-top", "0.4rem")
                    .set("font-size", "0.85rem")
                    .set("color", "#1d4ed8");
            content.add(summary);
        }
        return panel("今日课表", content, () -> openCourses(clazz));
    }

    // ==================== 食堂 ====================

    private Component canteenPanel() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        List<CanteenMenu> todayMenu = canteenService.todayMenu();
        if (todayMenu.isEmpty()) {
            content.add(new Span("今天食堂不供应餐食"));
            return panel("今日食堂", content, this::openCanteen);
        }
        for (CanteenMenu menu : todayMenu) {
            VerticalLayout row = new VerticalLayout();
            row.setPadding(false);
            row.setSpacing(false);
            row.getStyle().set("border-bottom", "1px dashed #e2e8f0").set("padding-bottom", "0.35rem");

            HorizontalLayout head = new HorizontalLayout();
            head.setSpacing(true);
            head.setAlignItems(FlexComponent.Alignment.CENTER);

            Span meal = new Span(menu.getMeal().getLabel());
            meal.getStyle().set("font-weight", "700").set("font-size", "0.9rem").set("color", "#b45309");
            Span price = new Span(menu.getPrice() == null ? "" : menu.getPrice());
            price.getStyle().set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)");
            head.add(meal, price);

            Span dishes = new Span(menu.getDishes() == null ? "" : menu.getDishes());
            dishes.getStyle().set("font-size", "0.86rem").set("line-height", "1.4");

            row.add(head, dishes);
            content.add(row);
        }
        return panel("今日食堂", content, this::openCanteen);
    }

    // ==================== 精选新闻 ====================

    private Component newsPanel() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        List<NewsItem> news = newsService.latest(4);
        if (news.isEmpty()) {
            content.add(new Span("暂无新闻"));
            return panel("精选新闻", content, this::openNews);
        }
        for (NewsItem item : news) {
            VerticalLayout texts = new VerticalLayout();
            texts.setPadding(false);
            texts.setSpacing(false);

            Span headline = new Span(item.getTitle());
            headline.getStyle().set("font-size", "0.92rem").set("font-weight", "600");

            Span meta = new Span((item.getSource() == null ? "" : item.getSource())
                    + " · " + (item.getPublishedAt() == null ? "" : item.getPublishedAt().format(TIME_FMT)));
            meta.getStyle().set("font-size", "0.76rem").set("color", "var(--lumo-secondary-text-color)");

            texts.add(headline, meta);

            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
                Image thumb = new Image(item.getImageUrl(), item.getTitle());
                thumb.getStyle()
                        .set("width", "48px")
                        .set("height", "48px")
                        .set("object-fit", "cover")
                        .set("border-radius", "6px")
                        .set("flex-shrink", "0");
                row.add(thumb);
            }
            row.add(texts);
            row.expand(texts);
            row.getStyle().set("border-bottom", "1px dashed #e2e8f0").set("padding-bottom", "0.3rem");
            content.add(row);
        }
        return panel("精选新闻", content, this::openNews);
    }

    // ==================== 今日歌单 ====================

    private Component songPanel(ClassInfo clazz) {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        long rejected = songService.forClass(clazz.getId()).stream()
                .filter(request -> request.getStatus() == SongStatus.REJECTED
                        && request.getRejectReadAt() == null)
                .count();
        if (rejected > 0) {
            Span notice = new Span("有 " + rejected + " 条点歌未通过待确认，点击查看审核通知");
            notice.getStyle()
                    .set("background", "#fef2f2")
                    .set("color", "#991b1b")
                    .set("border-radius", "8px")
                    .set("padding", "0.4rem 0.7rem")
                    .set("font-size", "0.85rem");
            content.add(notice);
        }

        List<SongRequest> playlist = songService.playlist();
        if (playlist.isEmpty()) {
            content.add(new Span("今日还没有排歌，去「广播点歌」点一首吧"));
            return panel("今日歌单", content, this::openSongs);
        }
        playlist.stream().limit(5).forEach(request -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);

            Span song = new Span(request.getSong());
            song.getStyle().set("font-weight", "600").set("font-size", "0.9rem");

            Span artist = new Span(request.getArtist() == null ? "" : "- " + request.getArtist());
            artist.getStyle().set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)");

            Span status = new Span(request.getStatus() == SongStatus.PLAYED ? "已播放" : "待播");
            status.getStyle()
                    .set("font-size", "0.72rem")
                    .set("padding", "0.1rem 0.4rem")
                    .set("border-radius", "999px")
                    .set("background", request.getStatus() == SongStatus.PLAYED ? "#dcfce7" : "#dbeafe")
                    .set("color", request.getStatus() == SongStatus.PLAYED ? "#166534" : "#1d4ed8");

            row.add(song, artist, status);
            row.setFlexGrow(1, song);
            content.add(row);
        });
        return panel("今日歌单", content, this::openSongs);
    }

    // ==================== 倒计时 / 值日 / 生日 ====================

    private Component lifePanel(ClassInfo clazz) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        List<UpcomingEvent> events = upcomingEvents(clazz);
        if (events.isEmpty()) {
            content.add(new Span("暂无考试或活动安排"));
        } else {
            events.stream().limit(3).forEach(event -> content.add(eventBlock(event)));
        }

        dutyService.todayForClass(clazz.getId()).ifPresent(roster -> content.add(dutyBlock(roster)));

        List<String> birthdays = studentService.birthdaysToday(clazz.getId()).stream()
                .map(student -> student.getName()).toList();
        if (!birthdays.isEmpty()) {
            Span birthday = new Span("🎂 生日快乐：" + String.join("、", birthdays));
            birthday.getStyle()
                    .set("background", "#fef3c7")
                    .set("color", "#92400e")
                    .set("border-radius", "10px")
                    .set("padding", "0.5rem 0.75rem")
                    .set("font-size", "0.88rem");
            content.add(birthday);
        }

        if (content.getComponentCount() == 0) {
            content.add(new Span("今天没有特别安排"));
        }
        return panel("班级生活", content, () -> openLife(clazz));
    }

    /** 合并「通知里的相关日期」与独立倒计时，按日期去重排序 */
    private List<UpcomingEvent> upcomingEvents(ClassInfo clazz) {
        LocalDate today = LocalDate.now();
        List<UpcomingEvent> events = new ArrayList<>();

        announcementService.findForClass(clazz.getId()).stream()
                .filter(announcement -> announcement.getEventDate() != null
                        && !announcement.getEventDate().isBefore(today))
                .forEach(announcement -> addEvent(events,
                        new UpcomingEvent(announcement.getTitle(), announcement.getEventDate(), true)));

        countdownService.findAll().stream()
                .filter(event -> event.getEventDate() != null && !event.getEventDate().isBefore(today))
                .forEach(event -> addEvent(events,
                        new UpcomingEvent(event.getName(), event.getEventDate(), false)));

        return events.stream()
                .sorted(Comparator.comparing(UpcomingEvent::date))
                .limit(5)
                .toList();
    }

    /** 同一天且名称互相包含的倒计时视为同一件事，避免通知和倒计时重复 */
    private void addEvent(List<UpcomingEvent> events, UpcomingEvent candidate) {
        boolean duplicate = events.stream().anyMatch(existing -> existing.date().equals(candidate.date())
                && (existing.name().contains(candidate.name()) || candidate.name().contains(existing.name())));
        if (!duplicate) {
            events.add(candidate);
        }
    }

    private VerticalLayout eventBlock(UpcomingEvent event) {
        VerticalLayout block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle()
                .set("background", "#eff6ff")
                .set("border-radius", "10px")
                .set("padding", "0.5rem 0.75rem");

        Span name = new Span(event.name());
        name.getStyle().set("font-weight", "700").set("color", "#1d4ed8");

        long days = ChronoUnit.DAYS.between(LocalDate.now(), event.date());
        Span remaining = new Span(days == 0 ? "就是今天" : "还有 " + days + " 天");
        remaining.getStyle().set("font-size", "1.05rem").set("font-weight", "700");

        Span date = new Span(event.date().format(DateTimeFormatter.ofPattern("MM-dd"))
                + (event.fromAnnouncement() ? " · 来自通知" : ""));
        date.getStyle().set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)");

        block.add(name, remaining, date);
        return block;
    }

    /** 班级生活弹窗里展示的日程，包含通知中的考试 / 活动和独立倒计时 */
    private record UpcomingEvent(String name, LocalDate date, boolean fromAnnouncement) {
    }

    private VerticalLayout dutyBlock(DutyRoster roster) {
        VerticalLayout block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle()
                .set("background", "#f8fafc")
                .set("border-radius", "10px")
                .set("padding", "0.5rem 0.75rem");

        Span title = new Span("今日值日");
        title.getStyle().set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)");
        Span students = new Span(roster.getStudents() == null ? "未安排" : roster.getStudents());
        students.getStyle().set("font-weight", "600");
        Span task = new Span(roster.getTask() == null ? "" : roster.getTask());
        task.getStyle().set("font-size", "0.78rem").set("color", "var(--lumo-secondary-text-color)");

        block.add(title, students, task);
        return block;
    }

    // ==================== 模块弹窗 ====================

    /** 点击班牌上的模块卡片后，用弹窗展示该模块的完整内容 */
    private void openModuleDialog(String title, Component body, String width, boolean fixedHeight) {
        hideModuleTitle(body);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setWidth(width);
        dialog.setMaxWidth("95vw");
        if (fixedHeight) {
            dialog.setHeight("82vh");
        }
        dialog.add(body);

        Button close = new Button("关闭", event -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(close);

        // 弹窗里的「查看详情」等操作会跳转页面，跳转前先关掉弹窗，避免遮罩留在新页面上
        Registration navigationClose = UI.getCurrent().addBeforeEnterListener(event -> dialog.close());
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                navigationClose.remove();
            }
        });
        dialog.open();
    }

    /** 复用整页视图时去掉视图自带的标题，标题统一由弹窗头部显示 */
    private void hideModuleTitle(Component component) {
        Component root = component instanceof Composite<?> composite ? composite.getContent() : component;
        root.getChildren()
                .filter(H2.class::isInstance)
                .forEach(child -> child.setVisible(false));
    }

    private void openAnnouncements() {
        openModuleDialog("通知公告", new AnnouncementView(announcementService), "920px", true);
    }

    private void openCheckIn(ClassInfo clazz) {
        CheckInView view = new CheckInView(checkInService, classInfoService);
        view.getStyle().set("overflow-y", "auto");
        openModuleDialog("校园卡签到 · " + clazz.getName(), view, "820px", true);
    }

    private void openCourses(ClassInfo clazz) {
        CourseView courseView = new CourseView(classInfoService, courseService);
        HomeworkView homeworkView = new HomeworkView(homeworkService);
        hideModuleTitle(courseView);
        hideModuleTitle(homeworkView);

        TabSheet tabs = new TabSheet();
        tabs.add("本周课表", courseView);
        tabs.add("今日作业", homeworkView);
        tabs.setSizeFull();

        openModuleDialog("班级课表 · " + clazz.getName(), tabs, "1020px", true);
    }

    private void openCanteen() {
        openModuleDialog("食堂菜单 · 本周", new CanteenView(canteenService), "1000px", true);
    }

    private void openNews() {
        openModuleDialog("精选新闻", new NewsView(newsService), "960px", true);
    }

    private void openSongs() {
        openModuleDialog("广播点歌", new SongView(songService), "960px", true);
    }

    private void openWeather() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        List<WeatherForecast> forecasts = weatherService.forecast();
        if (forecasts.isEmpty()) {
            content.add(new Span("暂时没有天气数据，请稍后再试。"));
            openModuleDialog("天气 · " + weatherService.getCity(), content, "720px", false);
            return;
        }

        forecasts.forEach(forecast -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);
            row.getStyle()
                    .set("background", "#f8fafc")
                    .set("border-radius", "12px")
                    .set("padding", "0.9rem 1.1rem");

            Span day = new Span(dayLabel(forecast.getForecastDate()));
            day.getStyle().set("font-weight", "700").set("min-width", "128px");

            Span condition = new Span(forecast.getCondition());
            condition.getStyle()
                    .set("font-size", "1.35rem")
                    .set("font-weight", "700")
                    .set("color", "#1d4ed8")
                    .set("min-width", "84px");

            Span temperature = new Span(temp(forecast.getTempLow()) + " ~ " + temp(forecast.getTempHigh()) + "℃");
            temperature.getStyle().set("font-size", "1.05rem").set("font-weight", "600").set("min-width", "132px");

            Span tip = new Span(forecast.getTip() == null ? "" : forecast.getTip());
            tip.getStyle().set("font-size", "0.88rem").set("color", "var(--lumo-secondary-text-color)");

            row.add(day, condition, temperature, tip);
            row.expand(tip);
            content.add(row);
        });

        weatherService.today()
                .map(WeatherForecast::getUpdatedAt)
                .ifPresent(updated -> {
                    Span update = new Span("数据更新于 " + updated.format(TIME_FMT));
                    update.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-tertiary-text-color)");
                    content.add(update);
                });

        openModuleDialog("天气 · " + weatherService.getCity() + " · 未来三天", content, "780px", false);
    }

    private String dayLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        String prefix = date.equals(today) ? "今天"
                : date.equals(today.plusDays(1)) ? "明天"
                : date.equals(today.plusDays(2)) ? "后天" : "未来";
        return prefix + " " + date.format(DateTimeFormatter.ofPattern("MM-dd"));
    }

    private void openLife(ClassInfo clazz) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        content.add(sectionTitle("倒计时 / 通知日程"));
        List<UpcomingEvent> events = upcomingEvents(clazz);
        if (events.isEmpty()) {
            content.add(new Span("暂无考试或活动倒计时"));
        } else {
            events.forEach(event -> {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                row.setSpacing(true);
                row.getStyle()
                        .set("background", "#eff6ff")
                        .set("border-radius", "10px")
                        .set("padding", "0.65rem 0.9rem");

                Span name = new Span(event.name());
                name.getStyle().set("font-weight", "600");
                Span date = new Span(event.date().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        + (event.fromAnnouncement() ? " · 通知" : ""));
                date.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");
                long days = ChronoUnit.DAYS.between(LocalDate.now(), event.date());
                Span remaining = new Span(days == 0 ? "就是今天" : "还有 " + days + " 天");
                remaining.getStyle().set("font-weight", "700").set("color", "#1d4ed8");

                row.add(name, date, remaining);
                row.expand(name);
                content.add(row);
            });
        }

        content.add(sectionTitle("本周值日"));
        List<DutyRoster> rosters = dutyService.forClass(clazz.getId());
        if (rosters.isEmpty()) {
            content.add(new Span("本周还没有安排值日"));
        } else {
            for (int day = 1; day <= 7; day++) {
                final int dayOfWeek = day;
                rosters.stream()
                        .filter(roster -> roster.getDayOfWeek() != null && roster.getDayOfWeek() == dayOfWeek)
                        .findFirst()
                        .ifPresent(roster -> {
                            HorizontalLayout row = new HorizontalLayout();
                            row.setWidthFull();
                            row.setAlignItems(FlexComponent.Alignment.CENTER);
                            row.setSpacing(true);
                            row.getStyle().set("border-bottom", "1px dashed #e2e8f0").set("padding", "0.4rem 0");

                            Span week = new Span(CourseService.dayName(dayOfWeek));
                            week.getStyle().set("font-weight", "600").set("min-width", "56px");
                            Span students = new Span(roster.getStudents() == null ? "未安排" : roster.getStudents());
                            Span task = new Span(roster.getTask() == null ? "" : roster.getTask());
                            task.getStyle().set("font-size", "0.82rem").set("color", "var(--lumo-secondary-text-color)");

                            row.add(week, students, task);
                            row.expand(students);
                            content.add(row);
                        });
            }
        }

        content.add(sectionTitle("今日生日"));
        List<String> birthdays = studentService.birthdaysToday(clazz.getId()).stream()
                .map(student -> student.getName())
                .toList();
        Span birthday = new Span(birthdays.isEmpty()
                ? "今天没有同学过生日"
                : "🎂 " + String.join("、", birthdays) + "，生日快乐！");
        birthday.getStyle()
                .set("background", "#fef3c7")
                .set("color", "#92400e")
                .set("border-radius", "10px")
                .set("padding", "0.65rem 0.9rem");
        content.add(birthday);

        openModuleDialog("班级生活 · " + clazz.getName(), content, "820px", true);
    }

    private H3 sectionTitle(String text) {
        H3 heading = new H3(text);
        heading.getStyle().set("margin", "0.6rem 0 0").set("font-size", "1.05rem");
        return heading;
    }

    // ==================== 通用面板 ====================

    private Component panel(String title, Component content, Runnable onClick) {
        H3 heading = new H3(title);
        heading.getStyle().set("margin", "0").set("font-size", "1.1rem");

        Div accent = new Div();
        accent.getStyle().set("width", "4px").set("height", "20px").set("background", "#2563eb")
                .set("border-radius", "2px");

        Span more = new Span("查看详情 ›");
        more.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#2563eb")
                .set("white-space", "nowrap");

        HorizontalLayout header = new HorizontalLayout(accent, heading, more);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.expand(heading);
        header.getStyle().set("flex-shrink", "0");

        VerticalLayout card = new VerticalLayout(header, content);
        card.setPadding(true);
        card.setSpacing(true);
        card.setSizeFull();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 18px rgba(15,23,42,0.08)")
                .set("overflow", "hidden")
                .set("min-height", "0");

        // 卡片高度固定后，内容超出时在卡片内部滚动，保证所有卡片大小一致
        content.getElement().getStyle()
                .set("overflow-y", "auto")
                .set("min-height", "0")
                .set("width", "100%");
        card.setFlexGrow(1, content);

        Div wrapper = new Div(card);
        wrapper.addClassName("cb-module-card");
        wrapper.setSizeFull();
        wrapper.getStyle()
                .set("cursor", "pointer")
                .set("border-radius", "16px")
                .set("outline", "none")
                .set("min-height", "0");
        wrapper.getElement().setAttribute("role", "button");
        wrapper.getElement().setAttribute("tabindex", "0");
        wrapper.getElement().setAttribute("title", "点击查看" + title + "详情");
        wrapper.addClickListener(event -> onClick.run());
        wrapper.getElement().addEventListener("keydown", event -> onClick.run())
                .setFilter("event.key === 'Enter' || event.key === ' '");
        return wrapper;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
