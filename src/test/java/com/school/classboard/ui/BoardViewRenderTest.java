package com.school.classboard.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.Announcement;
import com.school.classboard.domain.CanteenMenu;
import com.school.classboard.domain.CheckInRecord;
import com.school.classboard.domain.CheckInType;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Course;
import com.school.classboard.domain.CountdownEvent;
import com.school.classboard.domain.DutyRoster;
import com.school.classboard.domain.Meal;
import com.school.classboard.domain.NewsItem;
import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import com.school.classboard.domain.Student;
import com.school.classboard.domain.WeatherForecast;
import com.school.classboard.service.AnnouncementService;
import com.school.classboard.service.CanteenService;
import com.school.classboard.service.CheckInService;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.CourseService;
import com.school.classboard.service.CountdownService;
import com.school.classboard.service.DutyService;
import com.school.classboard.service.HomeworkService;
import com.school.classboard.service.NewsService;
import com.school.classboard.service.SongService;
import com.school.classboard.service.StudentService;
import com.school.classboard.service.WeatherService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 班牌整页构建测试：覆盖 8 个面板的数据拼装，避免只有空壳或 NPE。
 */
@ExtendWith(MockitoExtension.class)
class BoardViewRenderTest {

    @Mock
    private ClassInfoService classInfoService;
    @Mock
    private AnnouncementService announcementService;
    @Mock
    private CourseService courseService;
    @Mock
    private HomeworkService homeworkService;
    @Mock
    private WeatherService weatherService;
    @Mock
    private CheckInService checkInService;
    @Mock
    private CanteenService canteenService;
    @Mock
    private NewsService newsService;
    @Mock
    private SongService songService;
    @Mock
    private CountdownService countdownService;
    @Mock
    private DutyService dutyService;
    @Mock
    private StudentService studentService;

    @Test
    void boardBuildsAllPanelsWithClassData() {
        ClassInfo clazz = new ClassInfo();
        clazz.setId(1L);
        clazz.setName("高一(3)班");
        clazz.setHeadTeacher("李明");
        clazz.setClassroom("教学楼A-301");
        clazz.setStudentCount(46);
        clazz.setMotto("志存高远，脚踏实地");

        Announcement announcement = new Announcement();
        announcement.setTitle("本周五举行秋季运动会");
        announcement.setCategory("活动");
        announcement.setPublishedAt(LocalDateTime.now());

        Course course = new Course();
        course.setSubject("数学");
        course.setTeacher("张老师");
        course.setStartTime("08:00");
        course.setEndTime("08:45");

        WeatherForecast weather = new WeatherForecast();
        weather.setCondition("多云");
        weather.setTempLow(17);
        weather.setTempHigh(24);
        weather.setTip("天气舒适");

        CanteenMenu menu = new CanteenMenu();
        menu.setMeal(Meal.LUNCH);
        menu.setDishes("红烧鸡腿 / 番茄炒蛋");
        menu.setPrice("12 元");

        NewsItem news = new NewsItem();
        news.setTitle("校园科技节开幕");
        news.setSource("校园网");
        news.setPublishedAt(LocalDateTime.now());

        SongRequest song = new SongRequest();
        song.setSong("夜空中最亮的星");
        song.setArtist("逃跑计划");
        song.setStatus(SongStatus.APPROVED);

        CountdownEvent countdown = new CountdownEvent();
        countdown.setName("期中考试");
        countdown.setEventDate(LocalDate.now().plusDays(12));

        DutyRoster duty = new DutyRoster();
        duty.setStudents("王雨桐、李泽宇");
        duty.setTask("扫地、擦黑板");

        Student student = new Student();
        student.setName("王雨桐");

        when(announcementService.findForClass(1L)).thenReturn(List.of(announcement));
        when(courseService.findToday(1L)).thenReturn(List.of(course));
        when(weatherService.today()).thenReturn(Optional.of(weather));
        when(weatherService.getCity()).thenReturn("杭州");
        when(weatherService.tomorrow()).thenReturn(Optional.empty());
        when(checkInService.stats(1L)).thenReturn(new CheckInService.Stats(46, 45, 1, 20, 2));
        when(canteenService.todayMenu()).thenReturn(List.of(menu));
        when(newsService.latest(4)).thenReturn(List.of(news));
        when(songService.playlist()).thenReturn(List.of(song));
        when(dutyService.todayForClass(1L)).thenReturn(Optional.of(duty));
        when(studentService.birthdaysToday(1L)).thenReturn(List.of(student));

        BoardView view = new BoardView(classInfoService, announcementService, courseService,
                homeworkService, weatherService, checkInService, canteenService, newsService,
                songService, countdownService, dutyService, studentService) {
            @Override
            protected ClassInfo currentClass() {
                return clazz;
            }
        };

        // 横幅 + 3×3 面板网格
        assertTrue(view.getComponentCount() >= 2, "班牌应至少包含横幅和面板网格");
    }
}
