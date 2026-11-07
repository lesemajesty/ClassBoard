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
 * 班级信息。
 */
@Entity
@Table(name = "class_info")
@Getter
@Setter
public class ClassInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 班级名称，如：高一(3)班 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 年级，如：2025级 */
    @Column(length = 32)
    private String grade;

    /** 班主任 */
    @Column(length = 32)
    private String headTeacher;

    /** 教室，如：教学楼A-301 */
    @Column(length = 64)
    private String classroom;

    /** 班级格言 */
    @Column(length = 128)
    private String motto;

    /** 班级合影图片地址 */
    @Column(length = 255)
    private String photoUrl;

    /** 学生人数 */
    private Integer studentCount;
}
