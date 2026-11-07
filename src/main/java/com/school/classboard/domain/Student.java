package com.school.classboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * 学生信息与校园卡绑定。卡号即刷卡时读到的号码。
 */
@Entity
@Table(name = "student")
@Getter
@Setter
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属班级 id */
    @Column(nullable = false)
    private Long classId;

    @Column(nullable = false, length = 32)
    private String name;

    /** 校园卡号，刷卡时唯一识别 */
    @Column(nullable = false, unique = true, length = 32)
    private String cardNo;

    /** 生日，用于班牌生日祝福（只比较月日） */
    private LocalDate birthday;

    /** 是否启用（转学、挂失后可停用） */
    private boolean enabled = true;
}
