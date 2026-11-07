package com.school.classboard.config;

import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.CheckInType;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.CountdownEvent;
import com.school.classboard.domain.DutyRoster;
import com.school.classboard.domain.Meal;
import com.school.classboard.domain.NewsItem;
import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import com.school.classboard.domain.Student;
import com.school.classboard.domain.WeatherForecast;
import com.school.classboard.repository.CanteenMenuRepository;
import com.school.classboard.repository.ClassInfoRepository;
import com.school.classboard.repository.CountdownEventRepository;
import com.school.classboard.repository.DutyRosterRepository;
import com.school.classboard.repository.NewsItemRepository;
import com.school.classboard.repository.SongRequestRepository;
import com.school.classboard.repository.StudentRepository;
import com.school.classboard.repository.WeatherForecastRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 校园生活模块的演示数据（表为空才写入）。
 *
 * <p>包含：学生校园卡、生日、精选新闻、点歌申请、本周食堂菜单、天气兜底数据、
 * 考试倒计时与值日表。
 */
@Component
@Order(2)
public class LifeDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LifeDataInitializer.class);

    private final ClassInfoRepository classInfoRepository;
    private final StudentRepository studentRepository;
    private final NewsItemRepository newsRepository;
    private final SongRequestRepository songRepository;
    private final CanteenMenuRepository canteenRepository;
    private final WeatherForecastRepository weatherRepository;
    private final CountdownEventRepository countdownRepository;
    private final DutyRosterRepository dutyRepository;

    public LifeDataInitializer(ClassInfoRepository classInfoRepository,
                               StudentRepository studentRepository,
                               NewsItemRepository newsRepository,
                               SongRequestRepository songRepository,
                               CanteenMenuRepository canteenRepository,
                               WeatherForecastRepository weatherRepository,
                               CountdownEventRepository countdownRepository,
                               DutyRosterRepository dutyRepository) {
        this.classInfoRepository = classInfoRepository;
        this.studentRepository = studentRepository;
        this.newsRepository = newsRepository;
        this.songRepository = songRepository;
        this.canteenRepository = canteenRepository;
        this.weatherRepository = weatherRepository;
        this.countdownRepository = countdownRepository;
        this.dutyRepository = dutyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Long classId = classInfoRepository.findAll().stream()
                .findFirst().map(ClassInfo::getId).orElse(null);
        if (classId == null) {
            return;
        }

        seedStudents(classId);
        seedNews();
        seedSongs(classId);
        seedCanteen();
        seedWeather();
        seedCountdown();
        seedDuty(classId);
    }

    private void seedStudents(Long classId) {
        if (studentRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        String[][] rows = {
                {"王雨桐", "2009-03-18"},
                {"李泽宇", "2009-05-02"},
                {"陈思涵", "2009-07-21"},
                {"张浩然", "2009-09-09"},
                {"刘欣怡", "2009-11-30"},
                {"赵子轩", "2010-01-15"},
                {"周语彤", "2010-02-27"},
                {"吴梓豪", "2010-04-08"},
                {"徐若曦", "2010-06-19"},
                {"孙一鸣", "2010-08-26"}
        };
        for (int i = 0; i < rows.length; i++) {
            Student student = new Student();
            student.setClassId(classId);
            student.setName(rows[i][0]);
            student.setCardNo(String.format("20250301%02d", i + 1));
            LocalDate birthday = LocalDate.parse(rows[i][1]);
            // 让第一位同学生日落在今天，方便演示生日祝福
            if (i == 0) {
                birthday = LocalDate.of(birthday.getYear(), today.getMonthValue(), today.getDayOfMonth());
            }
            student.setBirthday(birthday);
            student.setEnabled(true);
            studentRepository.save(student);
        }
        log.info("已写入演示学生与校园卡数据，共 {} 人", rows.length);
    }

    private void seedNews() {
        if (newsRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        newsRepository.saveAll(List.of(
                news("校园科技节开幕，水火箭与机器人同台亮相",
                        "第十三届校园科技节在体育馆开幕，高一同学带来的水火箭、循迹机器人项目吸引了不少围观。",
                        "校园网", "校园", true, now.minusHours(3)),
                news("高一年级开展秋季研学：走进城市博物馆",
                        "同学们分组完成“城市记忆”主题探究任务，在实景中理解历史课本里的城市变迁。",
                        "德育处", "校园", false, now.minusDays(1)),
                news("“天宫课堂”新一期开讲，太空实验再次点燃好奇心",
                        "航天员在轨演示微重力下的流体与燃烧实验，学校组织各班在班会课集中观看。",
                        "中国载人航天", "科技", false, now.minusDays(2)),
                news("校园读书月推荐书单发布",
                        "语文组整理出 20 本适合高中生阅读的书籍，图书馆同步设置专题展架。",
                        "图书馆", "文化", false, now.minusDays(3))
        ));
        log.info("已写入演示精选新闻");
    }

    private void seedSongs(Long classId) {
        if (songRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        songRepository.saveAll(List.of(
                song(classId, "夜空中最亮的星", "逃跑计划", "王雨桐", "送给正在备考的大家，一起加油！", SongStatus.APPROVED, now.minusHours(4)),
                song(classId, "起风了", "买辣椒也用券", "李泽宇", "送给三班的每一位同学", SongStatus.PLAYED, now.minusHours(6)),
                song(classId, "平凡之路", "朴树", "陈思涵", "午休时间想听", SongStatus.PENDING, now.minusMinutes(40))
        ));
        log.info("已写入演示点歌申请");
    }

    private void seedCanteen() {
        if (canteenRepository.count() > 0) {
            return;
        }
        LocalDate weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        String[][] breakfast = {
                {"豆浆 / 鲜肉包 / 水煮蛋 / 凉拌黄瓜", "6 元"},
                {"小米粥 / 葱油饼 / 茶叶蛋 / 榨菜", "6 元"},
                {"牛奶 / 三明治 / 鸡蛋 / 水果", "8 元"},
                {"白粥 / 烧麦 / 卤蛋 / 小菜", "6 元"},
                {"豆花 / 油条 / 鸡蛋 / 凉拌木耳", "6 元"}
        };
        String[][] lunch = {
                {"红烧鸡腿 / 番茄炒蛋 / 清炒时蔬 / 米饭 / 紫菜蛋汤", "12 元"},
                {"糖醋里脊 / 麻婆豆腐 / 蒜蓉西兰花 / 米饭 / 冬瓜汤", "12 元"},
                {"土豆烧牛肉 / 鱼香肉丝 / 炒青菜 / 米饭 / 番茄蛋汤", "13 元"},
                {"宫保鸡丁 / 干煸豆角 / 清炒白菜 / 米饭 / 萝卜汤", "12 元"},
                {"红烧排骨 / 蒸蛋 / 上汤菠菜 / 米饭 / 银耳汤", "14 元"}
        };
        String[][] dinner = {
                {"香菇滑鸡 / 青椒土豆丝 / 炒油麦菜 / 米饭 / 例汤", "11 元"},
                {"鱼香茄子 / 木须肉 / 清炒生菜 / 米饭 / 例汤", "11 元"},
                {"咖喱鸡块 / 手撕包菜 / 醋溜白菜 / 米饭 / 例汤", "11 元"},
                {"回锅肉 / 蒜蓉粉丝娃娃菜 / 炒菠菜 / 米饭 / 例汤", "12 元"},
                {"红烧带鱼 / 家常豆腐 / 清炒菜心 / 米饭 / 例汤", "12 元"}
        };
        for (int day = 1; day <= 5; day++) {
            saveCanteen(weekStart, day, Meal.BREAKFAST, breakfast[day - 1][0], breakfast[day - 1][1]);
            saveCanteen(weekStart, day, Meal.LUNCH, lunch[day - 1][0], lunch[day - 1][1]);
            saveCanteen(weekStart, day, Meal.DINNER, dinner[day - 1][0], dinner[day - 1][1]);
        }
        log.info("已写入本周食堂菜单");
    }

    private void seedWeather() {
        if (weatherRepository.count() > 0) {
            return;
        }
        String[] conditions = {"多云", "晴", "小雨"};
        int[] lows = {17, 16, 15};
        int[] highs = {24, 26, 21};
        for (int i = 0; i < 3; i++) {
            WeatherForecast forecast = new WeatherForecast();
            forecast.setCity("杭州");
            forecast.setForecastDate(LocalDate.now().plusDays(i));
            forecast.setCondition(conditions[i]);
            forecast.setTempLow(lows[i]);
            forecast.setTempHigh(highs[i]);
            forecast.setTip(i == 2 ? "有雨，记得带伞" : "天气舒适，适合户外活动");
            forecast.setUpdatedAt(LocalDateTime.now());
            weatherRepository.save(forecast);
        }
        log.info("已写入演示天气数据");
    }

    private void seedCountdown() {
        if (countdownRepository.count() > 0) {
            return;
        }
        LocalDate now = LocalDate.now();
        countdownRepository.saveAll(List.of(
                countdown("期中考试", now.plusDays(12), "高一年级统一考试，注意复习节奏"),
                countdown("秋季运动会", now.plusDays(30), "各班准备班旗与入场口号"),
                countdown("高考", LocalDate.of(now.getYear() + 1, 6, 7), "为高三学长学姐加油")
        ));
        log.info("已写入倒计时事件");
    }

    private void seedDuty(Long classId) {
        if (dutyRepository.count() > 0) {
            return;
        }
        String[] groupA = {"王雨桐", "李泽宇", "陈思涵"};
        String[] groupB = {"张浩然", "刘欣怡", "赵子轩"};
        String[] groupC = {"周语彤", "吴梓豪", "徐若曦"};
        String[] groups = {null, join(groupA), join(groupB), join(groupC), join(groupA), join(groupB)};
        for (int day = 1; day <= 5; day++) {
            DutyRoster roster = new DutyRoster();
            roster.setClassId(classId);
            roster.setDayOfWeek(day);
            roster.setStudents(groups[day]);
            roster.setTask(day % 2 == 1 ? "扫地、拖地、擦黑板" : "倒垃圾、整理讲台、关灯关窗");
            dutyRepository.save(roster);
        }
        log.info("已写入班级值日表");
    }

    private String join(String[] names) {
        return String.join("、", names);
    }

    private NewsItem news(String title, String summary, String source, String category,
                          boolean featured, LocalDateTime publishedAt) {
        NewsItem item = new NewsItem();
        item.setTitle(title);
        item.setSummary(summary);
        item.setSource(source);
        item.setCategory(category);
        item.setFeatured(featured);
        item.setPublishedAt(publishedAt);
        item.setActive(true);
        return item;
    }

    private SongRequest song(Long classId, String name, String artist, String requester,
                             String reason, SongStatus status, LocalDateTime createdAt) {
        SongRequest request = new SongRequest();
        request.setClassId(classId);
        request.setSong(name);
        request.setArtist(artist);
        request.setRequester(requester);
        request.setReason(reason);
        request.setStatus(status);
        request.setCreatedAt(createdAt);
        if (status == SongStatus.PLAYED) {
            request.setPlayedAt(createdAt.plusMinutes(30));
        }
        return request;
    }

    private void saveCanteen(LocalDate weekStart, int day, Meal meal, String dishes, String price) {
        CanteenMenu menu = new CanteenMenu();
        menu.setWeekStart(weekStart);
        menu.setDayOfWeek(day);
        menu.setMeal(meal);
        menu.setDishes(dishes);
        menu.setPrice(price);
        canteenRepository.save(menu);
    }

    private CountdownEvent countdown(String name, LocalDate date, String note) {
        CountdownEvent event = new CountdownEvent();
        event.setName(name);
        event.setEventDate(date);
        event.setNote(note);
        return event;
    }
}
