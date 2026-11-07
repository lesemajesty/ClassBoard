package com.school.classboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * 食堂菜单：按「周 + 星期 + 餐次」维护，一周一份，可从上周末同步。
 */
@Entity
@Table(name = "canteen_menu",
        uniqueConstraints = @UniqueConstraint(columnNames = {"weekStart", "dayOfWeek", "meal"}))
@Getter
@Setter
public class CanteenMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 该周周一日期 */
    @Column(nullable = false)
    private LocalDate weekStart;

    /** 1=周一 ... 7=周日 */
    @Column(nullable = false)
    private Integer dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Meal meal;

    /** 菜品，用 / 分隔 */
    @Column(columnDefinition = "TEXT")
    private String dishes;

    /** 价格说明，如 12 元 */
    @Column(length = 32)
    private String price;

    @Column(length = 128)
    private String note;
}
