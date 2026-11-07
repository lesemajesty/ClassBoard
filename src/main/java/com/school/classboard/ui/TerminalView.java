package com.school.classboard.ui;

import com.school.classboard.domain.TerminalDevice;
import com.school.classboard.service.TerminalDeviceService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Optional;

/**
 * 班牌终端入口。把该地址设为一体机的开机首页即可，例如：
 * <pre>http://服务器地址/terminal/CB-A301-01</pre>
 * 访问后自动解析设备所绑定的班级并进入该班班牌，无需人工选择。
 */
@Route("terminal/:code")
@PageTitle("班牌终端")
@AnonymousAllowed
public class TerminalView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final TerminalDeviceService deviceService;

    public TerminalView(TerminalDeviceService deviceService) {
        this.deviceService = deviceService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setAlignItems(FlexComponent.Alignment.CENTER);
        root.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        root.getStyle().set("background", "linear-gradient(135deg, #1e3a8a 0%, #0ea5e9 100%)");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String code = event.getRouteParameters().get("code").orElse("");
        Optional<TerminalDevice> found = deviceService.findByCode(code);
        if (found.isEmpty()) {
            showProblem("设备未注册", "设备编码「" + code + "」不存在，请先在后台「设备管理」中登记该设备。");
            return;
        }

        TerminalDevice device = found.get();
        if (!device.isEnabled()) {
            showProblem("设备已停用", "设备「" + code + "」已被停用，请联系管理员。");
            return;
        }
        if (device.getClassId() == null) {
            showProblem("设备尚未绑定班级", "设备「" + code + "」还没有绑定班级，请先在后台「设备管理」中完成绑定。");
            return;
        }

        ClassSession.setClassId(device.getClassId());
        ClassSession.setDeviceCode(device.getCode());
        deviceService.touch(device.getId());
        event.rerouteTo(BoardView.class);
    }

    private void showProblem(String title, String detail) {
        H2 heading = new H2(title);
        heading.getStyle().set("margin", "0").set("color", "#ffffff");

        Span text = new Span(detail);
        text.getStyle().set("color", "rgba(255,255,255,0.9)");

        RouterLink toLogin = new RouterLink("前往登录页手动选择班级", LoginView.class);
        toLogin.getStyle().set("color", "#ffffff").set("text-decoration", "underline");

        VerticalLayout card = new VerticalLayout(heading, text, toLogin);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
                .set("background", "rgba(255,255,255,0.12)")
                .set("border-radius", "16px")
                .set("padding", "2rem");
        getContent().add(card);
    }
}
