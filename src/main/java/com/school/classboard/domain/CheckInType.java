package com.school.classboard.domain;

/** 校园卡签到类型：到校 / 离校 */
public enum CheckInType {
    IN("到校"),
    OUT("离校");

    private final String label;

    CheckInType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
