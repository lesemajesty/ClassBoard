package com.school.classboard.domain;

/** 广播站点歌申请状态 */
public enum SongStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    PLAYED("已播放"),
    REJECTED("已驳回");

    private final String label;

    SongStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
