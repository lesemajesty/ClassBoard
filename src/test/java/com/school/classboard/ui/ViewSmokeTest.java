package com.school.classboard.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.school.classboard.service.AnnouncementService;
import com.school.classboard.service.CanteenService;
import com.school.classboard.service.CheckInService;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.CourseService;
import com.school.classboard.service.CountdownService;
import com.school.classboard.service.DutyService;
import com.school.classboard.service.HomeworkService;
import com.school.classboard.service.ImageStorageService;
import com.school.classboard.service.LeaveService;
import com.school.classboard.service.NewsService;
import com.school.classboard.service.NewsSyncService;
import com.school.classboard.service.SongService;
import com.school.classboard.service.StudentService;
import com.school.classboard.service.TerminalDeviceService;
import com.school.classboard.service.WeatherService;
import com.school.classboard.ui.admin.CanteenAdminView;
import com.school.classboard.ui.admin.CheckInAdminView;
import com.school.classboard.ui.admin.HomeworkAdminView;
import com.school.classboard.ui.admin.LeaveAdminView;
import com.school.classboard.ui.admin.LifeAdminView;
import com.school.classboard.ui.admin.NewsAdminView;
import com.school.classboard.ui.admin.SongAdminView;
import com.school.classboard.ui.admin.StudentAdminView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 页面冒烟测试：Vaadin 视图在浏览器首次访问时才实例化，
 * 编译期发现不了构造器里的空指针/状态问题，这里直接构造一遍兜底。
 */
@ExtendWith(MockitoExtension.class)
class ViewSmokeTest {

    @Mock
    private ClassInfoService classInfoService;
    @Mock
    private AnnouncementService announcementService;
    @Mock
    private CourseService courseService;
    @Mock
    private TerminalDeviceService terminalDeviceService;
    @Mock
    private WeatherService weatherService;
    @Mock
    private CheckInService checkInService;
    @Mock
    private CanteenService canteenService;
    @Mock
    private NewsService newsService;
    @Mock
    private NewsSyncService newsSyncService;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private SongService songService;
    @Mock
    private CountdownService countdownService;
    @Mock
    private DutyService dutyService;
    @Mock
    private StudentService studentService;
    @Mock
    private HomeworkService homeworkService;
    @Mock
    private LeaveService leaveService;

    @Test
    void loginViewBuilds() {
        assertNotNull(new LoginView(classInfoService));
    }

    @Test
    void boardViewBuildsWithoutSelectedClass() {
        assertNotNull(new BoardView(classInfoService, announcementService, courseService,
                homeworkService, weatherService, checkInService, canteenService, newsService,
                songService, countdownService, dutyService, studentService));
    }

    @Test
    void checkInViewBuildsWithoutSelectedClass() {
        assertNotNull(new CheckInView(checkInService, classInfoService));
    }

    @Test
    void newsViewBuilds() {
        assertNotNull(new NewsView(newsService));
    }

    @Test
    void newsDetailViewBuilds() {
        assertNotNull(new NewsDetailView(newsService));
    }

    @Test
    void hybridActionViewBuilds() {
        assertNotNull(new HybridActionView());
    }

    @Test
    void canteenViewBuilds() {
        assertNotNull(new CanteenView(canteenService));
    }

    @Test
    void songViewBuilds() {
        assertNotNull(new SongView(songService));
    }

    @Test
    void studentAdminViewBuilds() {
        assertNotNull(new StudentAdminView(studentService, classInfoService));
    }

    @Test
    void checkInAdminViewBuilds() {
        assertNotNull(new CheckInAdminView(checkInService, classInfoService));
    }

    @Test
    void newsAdminViewBuilds() {
        assertNotNull(new NewsAdminView(newsService, newsSyncService, imageStorageService));
    }

    @Test
    void songAdminViewBuilds() {
        assertNotNull(new SongAdminView(songService, classInfoService));
    }

    @Test
    void canteenAdminViewBuilds() {
        assertNotNull(new CanteenAdminView(canteenService));
    }

    @Test
    void lifeAdminViewBuilds() {
        assertNotNull(new LifeAdminView(countdownService, dutyService, classInfoService));
    }

    @Test
    void homeworkViewBuildsWithoutSelectedClass() {
        assertNotNull(new HomeworkView(homeworkService));
    }

    @Test
    void leaveAdminViewBuilds() {
        assertNotNull(new LeaveAdminView(leaveService, classInfoService, studentService));
    }

    @Test
    void homeworkAdminViewBuilds() {
        assertNotNull(new HomeworkAdminView(homeworkService, classInfoService));
    }
}
