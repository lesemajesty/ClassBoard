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
 * 精选新闻：由后台维护的校园/时事新闻摘要，展示在班牌与新闻页。
 */
@Entity
@Table(name = "news_item")
@Getter
@Setter
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 来源，如：校园网、人民日报 */
    @Column(length = 64)
    private String source;

    /** 分类，如：校园、科技、时政、体育 */
    @Column(length = 32)
    private String category;

    @Column(length = 255)
    private String url;

    /** 配图地址：本地上传的 /uploads/xxx 或外部图片链接 */
    @Column(length = 500)
    private String imageUrl;

    /** 外部新闻唯一标识（来源 + 链接哈希），手工录入时为空 */
    @Column(unique = true, length = 128)
    private String externalKey;

    private LocalDateTime publishedAt;

    /** 是否推荐到班牌首页 */
    private boolean featured;

    private boolean active = true;
}
