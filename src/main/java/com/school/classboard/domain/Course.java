package com.school.classboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 课程表条目（某班级、某星期、某节次的课程）。
 */
@Entity
@Table(name = "course")
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属班级 id */
    @Column(nullable = false)
    private Long classId;

    /** 星期：1=周一 ... 7=周日 */
    @Column(nullable = false)
    private Integer dayOfWeek;

    /** 节次：1..N */
    @Column(nullable = false)
    private Integer period;

    /** 科目 */
    @Column(length = 32)
    private String subject;

    /** 任课教师 */
    @Column(length = 32)
    private String teacher;

    /** 开始时间，如 08:00 */
    @Column(length = 8)
    private String startTime;

    /** 结束时间，如 08:45 */
    @Column(length = 8)
    private String endTime;
}
