package com.school.classboard.ui;

import com.school.classboard.service.ClassInfoService;
import com.school.classboard.ui.admin.AnnouncementAdminView;
import com.school.classboard.ui.admin.CanteenAdminView;
import com.school.classboard.ui.admin.CheckInAdminView;
import com.school.classboard.ui.admin.ClassAdminView;
import com.school.classboard.ui.admin.CourseAdminView;
import com.school.classboard.ui.admin.DeviceAdminView;
import com.school.classboard.ui.admin.HomeworkAdminView;
import com.school.classboard.ui.admin.LeaveAdminView;
import com.school.classboard.ui.admin.LifeAdminView;
import com.school.classboard.ui.admin.NewsAdminView;
import com.school.classboard.ui.admin.SongAdminView;
import com.school.classboard.ui.admin.StudentAdminView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 全局主布局：顶部标题栏 + 实时时钟。
 *
 * <p>后台管理保留左侧导航；班级终端不再使用侧边栏，所有功能入口都放在班牌首页的模块卡片里，
 * 点击卡片以弹窗展示详情，因此终端只保留顶栏和「班牌首页 / 切换班级」两个快捷入口。
 */
@AnonymousAllowed
public class MainLayout extends AppLayout {

    /** 模块卡片悬浮效果；只注入一次，避免每次切换页面重复添加 style 标签 */
    private static final String MODULE_CARD_CSS = """
            .cb-module-card { transition: transform .16s ease, box-shadow .16s ease; }
            .cb-module-card:hover { transform: translateY(-2px); }
            .cb-module-card:active { transform: translateY(0); }

            /* 窄屏适配：两列卡片在手机上会挤到每列两百多像素，改成单列更好读 */
            @media (max-width: 860px) {
                .cb-module-grid { grid-template-columns: 1fr !important; }
            }
            @media (max-width: 720px) {
                .cb-navbar { padding: .35rem .65rem !important; gap: .3rem !important; }
                .cb-nav-left h1 { font-size: 1rem !important; letter-spacing: .5px !important; white-space: nowrap; }
                .cb-identity { display: none !important; }
                .cb-clock { font-size: .8rem !important; white-space: nowrap; }
                .cb-nav-right { gap: .2rem !important; }
                .cb-banner { flex-direction: column !important; align-items: flex-start !important;
                             padding: 1rem 1.1rem !important; gap: .7rem !important; }
                .cb-banner-left h2 { font-size: 1.9rem !important; letter-spacing: 1px !important; }
                .cb-banner-right { flex-wrap: wrap !important; gap: .4rem !important; }
            }
            @media (max-width: 520px) {
                .cb-clock { display: none !important; }
            }
            """;

    private static final String CLOCK_JS = """
            const el = this;
            const pad = n => String(n).padStart(2, '0');
            const week = ['日', '一', '二', '三', '四', '五', '六'];
            const render = () => {
                const d = new Date();
                el.textContent = d.getFullYear() + '年' + pad(d.getMonth() + 1) + '月' + pad(d.getDate())
                    + '日  星期' + week[d.getDay()] + '  ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
                    + ':' + pad(d.getSeconds());
            };
            render();
            // 布局被复用/重新挂载时先清掉旧计时器，避免秒表越走越多
            if (el.__cbClockTimer) {
                clearInterval(el.__cbClockTimer);
            }
            el.__cbClockTimer = setInterval(render, 1000);
            """;

    private final Span clock = new Span();

    public MainLayout(ClassInfoService classInfoService) {
        boolean admin = isLoggedIn();
        if (admin) {
            // 管理员界面保持原样：左侧导航完整保留
            setPrimarySection(Section.DRAWER);
            addToNavbar(createNavbar(classInfoService, true));
            addToDrawer(createDrawer());
        } else {
            // 班级终端不显示侧边栏，功能全部通过班牌首页的模块弹窗进入
            setPrimarySection(Section.NAVBAR);
            addToNavbar(createNavbar(classInfoService, false));
        }
    }

    private Component createNavbar(ClassInfoService classInfoService, boolean admin) {
        H1 title = new H1(admin ? "智慧电子班牌 · 管理后台" : "智慧电子班牌");
        title.getStyle()
                .set("font-size", "1.25rem")
                .set("margin", "0")
                .set("letter-spacing", "2px");

        HorizontalLayout left = new HorizontalLayout();
        if (admin) {
            left.add(new DrawerToggle());
        }
        left.add(title);
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.addClassName("cb-nav-left");

        Span identityLabel = new Span(identityLabel(classInfoService));
        identityLabel.addClassName("cb-identity");
        identityLabel.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "nowrap");

        clock.addClassName("cb-clock");
        clock.getStyle()
                .set("font-size", "1.05rem")
                .set("font-weight", "600")
                .set("font-variant-numeric", "tabular-nums");

        HorizontalLayout right = new HorizontalLayout(identityLabel, clock);
        right.addClassName("cb-nav-right");
        right.setAlignItems(FlexComponent.Alignment.CENTER);
        right.setSpacing(true);
        if (admin) {
            Button logout = new Button("退出登录");
            logout.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            logout.addClickListener(e -> UI.getCurrent().getPage().setLocation("logout"));
            right.add(logout);
        } else {
            if (ClassSession.getClassId() != null) {
                right.add(navButton("班牌首页", BoardView.class), navButton("切换班级", LoginView.class));
            } else {
                right.add(navButton("选择班级", LoginView.class));
            }
        }

        HorizontalLayout navbar = new HorizontalLayout(left, right);
        navbar.addClassName("cb-navbar");
        navbar.setWidthFull();
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setSpacing(true);
        navbar.expand(right);
        navbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        navbar.getStyle().set("padding", "0 1rem");
        return navbar;
    }

    private Component createDrawer() {
        VerticalLayout nav = new VerticalLayout();
        nav.setSizeFull();
        nav.setPadding(true);
        nav.setSpacing(false);

        addAdminMenu(nav);
        return nav;
    }

    /** 管理员只看到后台管理，不混入班牌端入口 */
    private void addAdminMenu(VerticalLayout nav) {
        nav.add(sectionLabel("后台管理"));
        nav.add(menuLink("公告发布", AnnouncementAdminView.class));
        nav.add(menuLink("班级管理", ClassAdminView.class));
        nav.add(menuLink("课表管理", CourseAdminView.class));
        nav.add(menuLink("作业管理", HomeworkAdminView.class));
        nav.add(menuLink("设备管理", DeviceAdminView.class));
        nav.add(menuLink("学生管理", StudentAdminView.class));
        nav.add(menuLink("签到记录", CheckInAdminView.class));
        nav.add(menuLink("请假登记", LeaveAdminView.class));
        nav.add(menuLink("新闻管理", NewsAdminView.class));
        nav.add(menuLink("点歌审核", SongAdminView.class));
        nav.add(menuLink("食堂菜单维护", CanteenAdminView.class));
        nav.add(menuLink("班级生活", LifeAdminView.class));
    }

    /** 教室终端顶栏快捷入口（终端没有侧边栏） */
    private Button navButton(String text, Class<? extends Component> viewClass) {
        Button button = new Button(text, e -> {
            UI ui = UI.getCurrent();
            // 已经在班牌首页时再点一次“班牌首页”，刷新页面并回到顶部，
            // 否则会看起来像点了没反应（尤其是页面已经向下滚动时）
            if (viewClass == BoardView.class && "board".equals(activePath(ui))) {
                ui.getPage().reload();
            } else {
                ui.navigate(viewClass);
            }
        });
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        return button;
    }

    private String activePath(UI ui) {
        var location = ui.getInternals().getActiveViewLocation();
        return location == null ? "" : location.getPath();
    }

    /** 顶栏身份：管理员显示账号，班牌终端显示班级与设备 */
    private String identityLabel(ClassInfoService classInfoService) {
        if (isLoggedIn()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return "管理员：" + auth.getName();
        }
        return terminalLabel(classInfoService);
    }

    private Div sectionLabel(String text) {
        Div divider = new Div();
        divider.setText(text);
        divider.getStyle()
                .set("font-size", "0.8rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "1rem 0 0.5rem");
        return divider;
    }

    private RouterLink menuLink(String text, Class<? extends Component> viewClass) {
        RouterLink link = new RouterLink(text, viewClass);
        link.getStyle()
                .set("padding", "0.6rem 0.75rem")
                .set("border-radius", "8px")
                .set("text-decoration", "none")
                .set("display", "block")
                .set("color", "var(--lumo-body-text-color)");
        return link;
    }

    private boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** 标题栏右侧的身份信息：当前班级 + 当前终端设备 */
    private String terminalLabel(ClassInfoService classInfoService) {
        StringBuilder label = new StringBuilder();
        Long classId = ClassSession.getClassId();
        if (classId != null) {
            classInfoService.findById(classId).ifPresent(c -> label.append("当前班级：").append(c.getName()));
        }
        String deviceCode = ClassSession.getDeviceCode();
        if (deviceCode != null) {
            if (label.length() > 0) {
                label.append("　");
            }
            label.append("终端：").append(deviceCode);
        }
        return label.toString();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        clock.getElement().executeJs(CLOCK_JS);
        getElement().executeJs("""
                if (!document.getElementById('cb-module-card-style')) {
                    const style = document.createElement('style');
                    style.id = 'cb-module-card-style';
                    style.textContent = $0;
                    document.head.appendChild(style);
                }
                """, MODULE_CARD_CSS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // 布局销毁时停掉秒表，避免定时器随着页面切换越积越多
        detachEvent.getUI().getPage().executeJs("clearInterval($0.__cbClockTimer)", clock.getElement());
        super.onDetach(detachEvent);
    }
}
