package com.school.classboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 班牌终端设备。每台一体机对应一条记录，通过设备编码绑定到某个班级。
 */
@Entity
@Table(name = "terminal_device")
@Getter
@Setter
public class TerminalDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备编码，终端访问地址 /terminal/{code} 中使用，如 CB-A301-01 */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 设备名称，如 教学楼A-301班牌 */
    @Column(length = 64)
    private String name;

    /** 安装位置 */
    @Column(length = 64)
    private String location;

    /** 绑定的班级 id，为空表示尚未绑定 */
    private Long classId;

    /** 是否启用 */
    private boolean enabled = true;

    /** 最近一次在线时间（终端访问时刷新） */
    private LocalDateTime lastSeenAt;
}
