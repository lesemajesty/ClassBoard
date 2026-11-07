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
 * 天气预报表：保存未来几天的天气，离线时继续展示最近一次数据。
 */
@Entity
@Table(name = "weather_forecast")
@Getter
@Setter
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String city;

    @Column(nullable = false, unique = true)
    private LocalDate forecastDate;

    /** 天气现象，如 晴 / 多云 / 小雨（condition 是 MySQL 保留字，列名加前缀） */
    @Column(name = "weather_condition", nullable = false, length = 32)
    private String condition;

    private Integer tempLow;

    private Integer tempHigh;

    /** 生活提示，如 有雨记得带伞 */
    @Column(length = 128)
    private String tip;

    private LocalDateTime updatedAt;
}
