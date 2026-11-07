package com.school.classboard.service;

import com.school.classboard.domain.NewsItem;
import com.school.classboard.repository.NewsItemRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 从新华网教育、人民网教育、中国教育新闻网抓取近期教育新闻。
 *
 * <p>只保留带日期的文章链接，按发布时间倒序取最新若干条；
 * 网络不可用时只记录日志，不影响已有新闻。
 */
@Service
public class NewsSyncService {

    private static final Logger log = LoggerFactory.getLogger(NewsSyncService.class);

    private static final Pattern ANCHOR = Pattern.compile("<a[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            Pattern.DOTALL);

    private static final String[][] SOURCES = {
            {"http://education.news.cn/", "新华网", "教育", "xinhua-edu"},
            {"http://edu.people.com.cn/", "人民网", "教育", "people-edu"},
            {"http://www.jyb.cn/", "中国教育新闻网", "教育", "jyb"}
    };

    private final NewsItemRepository repository;
    private final boolean enabled;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public NewsSyncService(NewsItemRepository repository,
                           @Value("${classboard.news.sync-enabled:true}") boolean enabled) {
        this.repository = repository;
        this.enabled = enabled;
    }

    record Draft(String key, String title, String summary, String url,
                 String source, String category, String imageUrl, LocalDateTime publishedAt) {
    }

    public record SyncResult(int added, int updated, int fetched) {
    }

    @Transactional
    public SyncResult sync() {
        return doSync();
    }

    @Scheduled(initialDelayString = "${classboard.news.initial-delay-ms:25000}",
            fixedDelayString = "${classboard.news.refresh-ms:1800000}")
    @Transactional
    public void scheduledSync() {
        if (enabled) {
            doSync();
        }
    }

    private SyncResult doSync() {
        List<Draft> drafts = new ArrayList<>();
        for (String[] source : SOURCES) {
            try {
                drafts.addAll(parseList(fetch(source[0]), source[0], source[1], source[2], source[3]));
            } catch (Exception e) {
                log.warn("{} 新闻同步失败：{}", source[1], e.getMessage());
            }
        }

        drafts.sort(Comparator.comparing(Draft::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (drafts.size() > 15) {
            drafts = new ArrayList<>(drafts.subList(0, 15));
        }

        int added = 0;
        int updated = 0;
        for (Draft draft : drafts) {
            if (isBlank(draft.key()) || isBlank(draft.title())) {
                continue;
            }
            NewsItem item = repository.findByExternalKey(draft.key()).orElseGet(NewsItem::new);
            boolean isNew = item.getId() == null;
            item.setExternalKey(draft.key());
            item.setTitle(trim(draft.title(), 128));
            item.setSummary(trim(draft.summary(), 2000));
            item.setUrl(trim(draft.url(), 255));
            item.setSource(trim(draft.source(), 64));
            item.setCategory(trim(draft.category(), 32));
            item.setImageUrl(trim(draft.imageUrl(), 500));
            item.setPublishedAt(draft.publishedAt() == null ? LocalDateTime.now() : draft.publishedAt());
            if (isNew) {
                item.setActive(true);
                item.setFeatured(false);
            }
            repository.save(item);
            if (isNew) {
                added++;
            } else {
                updated++;
            }
        }

        // 外部新闻保留最近 7 天，避免长期累积
        repository.deleteByExternalKeyIsNotNullAndPublishedAtBefore(LocalDateTime.now().minusDays(7));
        if (added > 0 || updated > 0) {
            log.info("新闻同步完成：新增 {} 条，更新 {} 条", added, updated);
        }
        return new SyncResult(added, updated, drafts.size());
    }

    private String fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * 从频道列表页解析文章链接。
     *
     * <p>只接受 URL 中带日期的链接，例如：
     * /20261019/xxx/c.html、/n1/2026/1018/c1006-xxx.html、/t20261019_xxx.html。
     */
    static List<Draft> parseList(String html, String baseUrl, String source,
                                 String category, String keyPrefix) {
        List<Draft> drafts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Matcher matcher = ANCHOR.matcher(html);
        while (matcher.find() && drafts.size() < 6) {
            String href = matcher.group(1);
            String title = clean(matcher.group(2));
            if (title.length() < 10 || title.length() > 80) {
                continue;
            }
            if (href.startsWith("#") || href.toLowerCase().contains("javascript")) {
                continue;
            }
            String url;
            try {
                url = URI.create(baseUrl).resolve(href).toString();
            } catch (Exception e) {
                continue;
            }
            if (!url.startsWith("http")) {
                continue;
            }
            LocalDate date = dateOf(url);
            if (date == null || date.isBefore(LocalDate.now().minusDays(60))) {
                continue;
            }
            if (!seen.add(url)) {
                continue;
            }
            drafts.add(new Draft(
                    keyPrefix + ":" + shortHash(url),
                    title,
                    "来自" + source + "的最新教育资讯，点击「查看原文」阅读全文。",
                    url,
                    source,
                    category,
                    "",
                    date.atStartOfDay()));
        }
        return drafts;
    }

    private static LocalDate dateOf(String url) {
        String[][] patterns = {
                {"/(20\\d{2})(\\d{2})(\\d{2})/", "ymd"},
                {"/(20\\d{2})/(\\d{2})/(\\d{2})/", "ymd"},
                {"/(20\\d{2})/(\\d{2})(\\d{2})/", "ymd"},
                {"t(20\\d{2})(\\d{2})(\\d{2})_", "ymd"}
        };
        for (String[] pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern[0]).matcher(url);
            if (matcher.find()) {
                try {
                    int year = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2));
                    int day = Integer.parseInt(matcher.group(3));
                    if (year >= 2020 && year <= 2030 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                        return LocalDate.of(year, month, day);
                    }
                } catch (Exception ignored) {
                    // 继续尝试下一个格式
                }
            }
        }
        return null;
    }

    private static String clean(String value) {
        return value.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}
