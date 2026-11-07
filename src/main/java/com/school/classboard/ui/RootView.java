package com.school.classboard.ui;

import com.school.classboard.ui.admin.AnnouncementAdminView;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 站点入口，按身份分流：
 * 已登录的后台管理员 → 后台管理；已选择班级的终端 → 班牌首页；否则 → 登录页。
 */
@Route("")
@AnonymousAllowed
public class RootView extends Composite<Div> implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (isAdminLoggedIn()) {
            event.rerouteTo(AnnouncementAdminView.class);
        } else if (ClassSession.getClassId() != null) {
            event.rerouteTo(BoardView.class);
        } else {
            event.rerouteTo(LoginView.class);
        }
    }

    private boolean isAdminLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
