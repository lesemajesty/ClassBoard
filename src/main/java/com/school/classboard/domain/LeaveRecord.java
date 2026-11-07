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
 * 学生请假记录。请假期间签到统计的“应到”会自动扣除。
 */
@Entity
@Table(name = "leave_record")
@Getter
@Setter
public class LeaveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    /** 姓名快照 */
    @Column(nullable = false, length = 32)
    private String studentName;

    @Column(nullable = false)
    private Long classId;

    /** 病假 / 事假 / 公假 */
    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 255)
    private String reason;

    /** 是否已批准；默认批准，驳回可由管理员删除 */
    private boolean approved = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
