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
 * 系统用户（后台管理员 / 教师）。
 */
@Entity
@Table(name = "sys_user")
@Getter
@Setter
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** BCrypt 加密后的密码 */
    @Column(nullable = false, length = 128)
    private String password;

    /** 显示名称 */
    @Column(length = 32)
    private String displayName;

    /** 角色：ADMIN / TEACHER */
    @Column(nullable = false, length = 32)
    private String role = "ADMIN";

    /** 是否启用 */
    private boolean enabled = true;
}
