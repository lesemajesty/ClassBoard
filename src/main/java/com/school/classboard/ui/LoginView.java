package com.school.classboard.ui;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.service.ClassInfoService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;

/**
 * 双入口登录页：
 * <ul>
 *   <li>班级班牌：免密码，选择班级即可进入该班的班牌终端界面</li>
 *   <li>后台管理：需要账号密码，由 Spring Security 校验</li>
 * </ul>
 */
@Route("login")
@PageTitle("登录 - 电子班牌")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final ClassInfoService classInfoService;
    private final LoginForm adminLogin = new LoginForm();

    public LoginView(ClassInfoService classInfoService) {
        this.classInfoService = classInfoService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #1e3a8a 0%, #0ea5e9 100%)");

        H1 title = new H1("智慧电子班牌");
        title.getStyle().set("color", "#ffffff").set("font-size", "2rem").set("margin", "0");

        Span subtitle = new Span("班级终端免密进入 · 后台管理需账号密码");
        subtitle.getStyle().set("color", "rgba(255,255,255,0.85)").set("font-size", "0.9rem");

        HorizontalLayout cards = new HorizontalLayout(buildClassCard(), buildAdminCard());
        cards.setSpacing(true);
        cards.setAlignItems(FlexComponent.Alignment.START);
        cards.getStyle().set("flex-wrap", "wrap").set("justify-content", "center");

        add(title, subtitle, cards);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            adminLogin.setError(true);
        }
    }

    /** 班级班牌入口：选择班级即可登录，无需密码 */
    private VerticalLayout buildClassCard() {
        VerticalLayout card = card("班级班牌");

        List<ClassInfo> classes = classInfoService.findAll();
        if (classes.isEmpty()) {
            card.add(new Span("暂无可选班级，请先用管理员账号进入后台添加班级"));
            return card;
        }

        ComboBox<ClassInfo> classSelect = new ComboBox<>("选择班级");
        classSelect.setItems(classes);
        classSelect.setItemLabelGenerator(c -> c.getName()
                + (c.getClassroom() == null ? "" : "（" + c.getClassroom() + "）"));
        classSelect.setWidth("300px");
        classSelect.addValueChangeListener(e -> {
            classSelect.setInvalid(false);
            classSelect.setErrorMessage(null);
        });

        Button enter = new Button("进入班牌", e -> {
            ClassInfo selected = classSelect.getValue();
            if (selected == null) {
                classSelect.setErrorMessage("请先选择班级");
                classSelect.setInvalid(true);
                return;
            }
            ClassSession.setClassId(selected.getId());
            UI.getCurrent().navigate(BoardView.class);
        });
        enter.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        enter.setWidth("300px");

        Span hint = new Span("教室终端从此入口进入，无需账号密码");
        hint.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)");

        card.add(classSelect, enter, hint);
        return card;
    }

    /** 后台管理入口：账号密码登录 */
    private VerticalLayout buildAdminCard() {
        VerticalLayout card = card("后台管理");

        adminLogin.setAction("login");
        adminLogin.setForgotPasswordButtonVisible(false);

        Span hint = new Span("默认管理员：admin / admin123");
        hint.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)");

        card.add(adminLogin, hint);
        return card;
    }

    private VerticalLayout card(String title) {
        H2 heading = new H2(title);
        heading.getStyle().set("margin", "0").set("font-size", "1.15rem");

        VerticalLayout card = new VerticalLayout(heading);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setSpacing(true);
        card.setPadding(true);
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "16px")
                .set("min-width", "340px")
                .set("box-shadow", "0 20px 40px rgba(0,0,0,0.25)");
        return card;
    }
}
