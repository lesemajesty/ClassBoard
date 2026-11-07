package com.school.classboard.domain;

/** 食堂餐次 */
public enum Meal {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐");

    private final String label;

    Meal(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
