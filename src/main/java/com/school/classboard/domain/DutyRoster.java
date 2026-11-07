package com.school.classboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * 班级值日表：每个班级每天一组值日生。
 */
@Entity
@Table(name = "duty_roster",
        uniqueConstraints = @UniqueConstraint(columnNames = {"classId", "dayOfWeek"}))
@Getter
@Setter
public class DutyRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long classId;

    /** 1=周一 ... 7=周日 */
    @Column(nullable = false)
    private Integer dayOfWeek;

    /** 值日生，用 / 或 、分隔 */
    @Column(length = 128)
    private String students;

    /** 值日内容，如 扫地、擦黑板、倒垃圾 */
    @Column(length = 128)
    private String task;
}
