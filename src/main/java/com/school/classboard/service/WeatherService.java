package com.school.classboard.service;

import com.school.classboard.domain.WeatherForecast;
import com.school.classboard.repository.WeatherForecastRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 天气服务：定时从 Open-Meteo 拉取未来三天天气（免密钥），
 * 拉取失败时保留上次数据；首次启动由演示数据兜底，因此断网也能正常展示。
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private static final String API = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=%s&longitude=%s"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
            + "&timezone=Asia%%2FShanghai&forecast_days=3";

    private final WeatherForecastRepository repository;
    private final boolean enabled;
    private final String city;
    private final double latitude;
    private final double longitude;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public WeatherService(WeatherForecastRepository repository,
                          @Value("${classboard.weather.enabled:true}") boolean enabled,
                          @Value("${classboard.weather.city:杭州}") String city,
                          @Value("${classboard.weather.latitude:30.2741}") double latitude,
                          @Value("${classboard.weather.longitude:120.1551}") double longitude) {
        this.repository = repository;
        this.enabled = enabled;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public List<WeatherForecast> forecast() {
        return repository.findByForecastDateGreaterThanEqualOrderByForecastDateAsc(LocalDate.now())
                .stream().limit(3).toList();
    }

    public Optional<WeatherForecast> today() {
        return repository.findByForecastDate(LocalDate.now());
    }

    public Optional<WeatherForecast> tomorrow() {
        return repository.findByForecastDate(LocalDate.now().plusDays(1));
    }

    /**
     * 每 30 分钟刷新一次；失败只记日志，不影响班牌展示。
     *
     * <p>事务注解必须放在定时方法上：scheduledRefresh() 直接调用 refresh() 属于自调用，
     * refresh() 上的 @Transactional 不会经过代理生效，批量删除旧天气会提示
     * "No EntityManager with actual transaction available"。
     */
    @Scheduled(initialDelayString = "${classboard.weather.initial-delay-ms:20000}",
            fixedDelayString = "${classboard.weather.refresh-ms:1800000}")
    @Transactional
    public void scheduledRefresh() {
        if (enabled) {
            refresh();
        }
    }

    @Transactional
    public void refresh() {
        try {
            URI uri = URI.create(String.format(Locale.ROOT, API, latitude, longitude));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "classboard/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("天气接口返回状态码 {}，继续使用本地数据", response.statusCode());
                return;
            }
            apply(response.body());
        } catch (Exception e) {
            log.warn("天气更新失败，继续使用本地数据：{}", e.getMessage());
        }
    }

    private void apply(String json) {
        List<String> dates = array(json, "time");
        List<String> codes = array(json, "weather_code");
        List<String> highs = array(json, "temperature_2m_max");
        List<String> lows = array(json, "temperature_2m_min");
        int size = Math.min(dates.size(), Math.min(codes.size(), Math.min(highs.size(), lows.size())));
        if (size == 0) {
            log.warn("天气接口没有返回有效预报");
            return;
        }

        repository.deleteByForecastDateBefore(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < size; i++) {
            LocalDate date = LocalDate.parse(dates.get(i));
            int code = (int) Double.parseDouble(codes.get(i));
            int high = (int) Math.round(Double.parseDouble(highs.get(i)));
            int low = (int) Math.round(Double.parseDouble(lows.get(i)));

            WeatherForecast forecast = repository.findByForecastDate(date)
                    .orElseGet(WeatherForecast::new);
            forecast.setCity(city);
            forecast.setForecastDate(date);
            forecast.setCondition(condition(code));
            forecast.setTempHigh(high);
            forecast.setTempLow(low);
            forecast.setTip(tip(code, high, low));
            forecast.setUpdatedAt(now);
            repository.save(forecast);
        }
        log.info("天气已更新：{} 未来 {} 天", city, size);
    }

    /** 从 Open-Meteo 的 JSON 中提取一维数组（避免为此引入 JSON 库） */
    private static List<String> array(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)]").matcher(json);
        if (!matcher.find()) {
            return List.of();
        }
        return Arrays.stream(matcher.group(1).split(","))
                .map(value -> value.replace("\"", "").trim())
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private static String condition(int code) {
        if (code == 0) {
            return "晴";
        }
        if (code <= 2) {
            return "多云";
        }
        if (code == 3) {
            return "阴";
        }
        if (code == 45 || code == 48) {
            return "雾";
        }
        if (code >= 51 && code <= 57) {
            return "毛毛雨";
        }
        if (code >= 61 && code <= 67) {
            return "小雨";
        }
        if (code >= 71 && code <= 77) {
            return "雪";
        }
        if (code >= 80 && code <= 82) {
            return "阵雨";
        }
        if (code >= 85 && code <= 86) {
            return "阵雪";
        }
        if (code >= 95) {
            return "雷阵雨";
        }
        return "多云";
    }

    private static String tip(int code, int high, int low) {
        if (code >= 95) {
            return "有雷阵雨，注意安全";
        }
        if (code >= 71 && code <= 86) {
            return "有雪，注意保暖防滑";
        }
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            return "有雨，记得带伞";
        }
        if (high >= 30) {
            return "天气炎热，注意补水";
        }
        if (low <= 5) {
            return "早晚较凉，注意添衣";
        }
        return "天气舒适，适合户外活动";
    }
}
