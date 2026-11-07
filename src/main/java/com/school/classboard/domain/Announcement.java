package com.school.classboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 通知公告。
 */
@Entity
@Table(name = "announcement")
@Getter
@Setter
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 标题 */
    @Column(nullable = false, length = 128)
    private String title;

    /** 正文 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 配图地址：本地上传的 /uploads/xxx 或外部图片链接 */
    @Column(length = 500)
    private String imageUrl;

    /** 分类：通知 / 活动 / 荣誉 / 紧急 */
    @Column(length = 32)
    private String category;

    /** 是否置顶 */
    private boolean pinned;

    /** 发布人 */
    @Column(length = 32)
    private String publisher;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 相关事件日期（考试 / 活动），用于班级生活倒计时 */
    private LocalDate eventDate;

    /** 自动回收时间，超过后公告自动下线；为空表示长期有效 */
    private LocalDateTime expireAt;

    /** 是否启用 */
    private boolean active = true;

    /** 适用班级 id，为 null 表示面向全校 */
    private Long classId;
}
