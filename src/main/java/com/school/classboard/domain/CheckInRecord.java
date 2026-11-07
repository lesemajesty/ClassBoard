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
 * 校园卡签到记录。学生每刷一次卡产生一条：首次为到校，第二次为离校。
 */
@Entity
@Table(name = "check_in_record")
@Getter
@Setter
public class CheckInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    /** 姓名快照，学生信息变动后历史记录仍可读 */
    @Column(nullable = false, length = 32)
    private String studentName;

    private Long classId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private CheckInType type;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    /** 是否迟到（仅到校记录有意义） */
    private boolean late;
}
