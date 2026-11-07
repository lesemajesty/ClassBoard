package com.school.classboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 广播站点歌申请：学生提交，广播室审核后排入歌单。
 */
@Entity
@Table(name = "song_request")
@Getter
@Setter
public class SongRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long classId;

    @Column(nullable = false, length = 64)
    private String song;

    @Column(length = 64)
    private String artist;

    /** 点歌理由 / 寄语 */
    @Column(length = 255)
    private String reason;

    /** 点歌人（可以是姓名或昵称） */
    @Column(length = 32)
    private String requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SongStatus status = SongStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 审核时间（通过、驳回或标记播放时更新） */
    private LocalDateTime reviewedAt;

    /** 驳回原因，驳回后保留并展示给点歌人 */
    @Column(length = 255)
    private String rejectReason;

    /** 学生点击“已读确认”的时间；为空表示这条驳回通知还未确认 */
    private LocalDateTime rejectReadAt;

    /** 实际播放时间，用于“今日歌单”排序 */
    private LocalDateTime playedAt;
}
