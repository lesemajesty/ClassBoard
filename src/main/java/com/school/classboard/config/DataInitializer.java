package com.school.classboard.config;

import com.school.classboard.domain.Announcement;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Course;
import com.school.classboard.domain.SysUser;
import com.school.classboard.domain.TerminalDevice;
import com.school.classboard.repository.AnnouncementRepository;
import com.school.classboard.repository.ClassInfoRepository;
import com.school.classboard.repository.CourseRepository;
import com.school.classboard.repository.SysUserRepository;
import com.school.classboard.repository.TerminalDeviceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首次启动时写入演示数据（表为空才写入，不会覆盖已有数据）。
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ClassInfoRepository classInfoRepository;
    private final AnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final SysUserRepository sysUserRepository;
    private final TerminalDeviceRepository terminalDeviceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ClassInfoRepository classInfoRepository,
                           AnnouncementRepository announcementRepository,
                           CourseRepository courseRepository,
                           SysUserRepository sysUserRepository,
                           TerminalDeviceRepository terminalDeviceRepository,
                           PasswordEncoder passwordEncoder) {
        this.classInfoRepository = classInfoRepository;
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.sysUserRepository = sysUserRepository;
        this.terminalDeviceRepository = terminalDeviceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (sysUserRepository.count() == 0) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setDisplayName("系统管理员");
            admin.setRole("ADMIN");
            sysUserRepository.save(admin);
            log.info("已创建默认管理员账号：admin / admin123");
        }

        if (classInfoRepository.count() == 0) {
            ClassInfo clazz = new ClassInfo();
            clazz.setName("高一(3)班");
            clazz.setGrade("2025级");
            clazz.setHeadTeacher("李明");
            clazz.setClassroom("教学楼A-301");
            clazz.setMotto("志存高远，脚踏实地");
            clazz.setStudentCount(46);
            classInfoRepository.save(clazz);

            seedCourses(clazz.getId());
            log.info("已写入演示班级与课表数据");
        }

        if (announcementRepository.count() == 0) {
            Long classId = classInfoRepository.findAll().stream().findFirst().map(ClassInfo::getId).orElse(null);
            LocalDateTime now = LocalDateTime.now();
            LocalDate examDate = LocalDate.now().plusDays(12);
            LocalDate sportsDate = LocalDate.now().plusDays(30);

            Announcement sports = newAnnouncement("秋季运动会安排已发布",
                    "秋季运动会定于 " + sportsDate.format(DateTimeFormatter.ofPattern("M 月 d 日"))
                            + " 全天在学校操场举行，请各班提前准备好班旗与口号，参赛同学注意做好热身。",
                    "活动", true, "教务处", now.minusHours(2), classId);
            sports.setEventDate(sportsDate);
            sports.setExpireAt(sportsDate.plusDays(1).atStartOfDay());

            Announcement exam = newAnnouncement("期中考试安排已发布",
                    "期中考试定于 " + examDate.format(DateTimeFormatter.ofPattern("M 月 d 日"))
                            + " 进行，具体科目与考场安排请查看班级群文件。",
                    "通知", false, "教务处", now.minusDays(1), null);
            exam.setEventDate(examDate);
            exam.setExpireAt(examDate.plusDays(1).atStartOfDay());

            Announcement award = newAnnouncement("我校学生在市科技创新大赛中获奖",
                    "热烈祝贺我班王同学在市级科技创新大赛中获得一等奖！",
                    "荣誉", false, "德育处", now.minusDays(2), null);
            award.setExpireAt(now.plusDays(30));

            announcementRepository.saveAll(List.of(sports, exam, award));
            log.info("已写入演示公告数据");
        }

        if (terminalDeviceRepository.count() == 0) {
            classInfoRepository.findAll().stream().findFirst().ifPresent(clazz -> {
                TerminalDevice device = new TerminalDevice();
                device.setCode("CB-A301-01");
                device.setName("教学楼A-301班牌");
                device.setLocation("教学楼A-301门口");
                device.setClassId(clazz.getId());
                device.setEnabled(true);
                terminalDeviceRepository.save(device);
                log.info("已写入演示终端设备，访问地址：/terminal/CB-A301-01");
            });
        }
    }

    private Announcement newAnnouncement(String title, String content, String category,
                                         boolean pinned, String publisher, LocalDateTime publishedAt, Long classId) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setCategory(category);
        a.setPinned(pinned);
        a.setPublisher(publisher);
        a.setPublishedAt(publishedAt);
        a.setActive(true);
        a.setClassId(classId);
        return a;
    }

    private void seedCourses(Long classId) {
        String[][] subjects = {
                {"语文", "数学", "英语", "物理", "化学", "生物", "体育"},
                {"数学", "英语", "语文", "历史", "地理", "政治", "自习"},
                {"英语", "物理", "数学", "化学", "语文", "信息技术", "自习"}
        };
        String[] startTimes = {"08:00", "08:55", "10:00", "10:55", "14:00", "14:55", "19:00"};
        String[] endTimes = {"08:45", "09:40", "10:45", "11:40", "14:45", "15:40", "19:45"};

        for (int day = 1; day <= 5; day++) {
            for (int period = 1; period <= 7; period++) {
                Course c = new Course();
                c.setClassId(classId);
                c.setDayOfWeek(day);
                c.setPeriod(period);
                c.setSubject(subjects[(day - 1) % subjects.length][period - 1]);
                c.setTeacher("张老师");
                c.setStartTime(startTimes[period - 1]);
                c.setEndTime(endTimes[period - 1]);
                courseRepository.save(c);
            }
        }
    }
}
