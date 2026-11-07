package com.school.classboard.ui;

import com.school.classboard.ui.admin.AnnouncementAdminView;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 浏览器插件 / 安全软件有时会向历史记录里塞入 hybridaction/xxx 这类伪地址，
 * Vaadin 会把它当成路由并显示 “Could not navigate to ...” 错误页。
 *
 * <p>这里注册一个兜底路由，命中后按身份直接导航到正确的首页，
 * 同时把地址栏里的伪地址替换掉。
 */
@Route("hybridaction/:action")
@AnonymousAllowed
public class HybridActionView extends Composite<Div> {

    public HybridActionView() {
        addAttachListener(event -> {
            UI ui = event.getUI();
            if (isLoggedIn()) {
                ui.navigate(AnnouncementAdminView.class);
            } else if (ClassSession.getClassId() != null) {
                ui.navigate(BoardView.class);
            } else {
                ui.navigate(LoginView.class);
            }
        });
    }

    private boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
