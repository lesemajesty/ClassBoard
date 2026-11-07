package com.school.classboard.ui;

import com.vaadin.flow.server.VaadinSession;

/**
 * 当前班牌终端所绑定的班级（会话级状态）。
 *
 * <p>班级账号是免密的：在登录页选择班级后即视为登录，把班级 id 存进会话即可。
 * 后台管理账号仍走 Spring Security 的账号密码验证。
 */
public final class ClassSession {

    private static final String CLASS_ID_KEY = "classboard.currentClassId";
    private static final String DEVICE_CODE_KEY = "classboard.currentDeviceCode";

    private ClassSession() {
    }

    public static Long getClassId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(CLASS_ID_KEY);
        return value instanceof Long id ? id : null;
    }

    public static void setClassId(Long classId) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(CLASS_ID_KEY, classId);
        }
    }

    /** 当前终端绑定的设备编码（由 /terminal/{code} 入口写入）；手动选班时为 null */
    public static String getDeviceCode() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(DEVICE_CODE_KEY);
        return value instanceof String code ? code : null;
    }

    public static void setDeviceCode(String code) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(DEVICE_CODE_KEY, code);
        }
    }
}
