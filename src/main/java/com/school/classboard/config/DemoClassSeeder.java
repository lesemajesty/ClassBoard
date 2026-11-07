package com.school.classboard.config;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Course;
import com.school.classboard.domain.DutyRoster;
import com.school.classboard.domain.Homework;
import com.school.classboard.domain.Student;
import com.school.classboard.repository.ClassInfoRepository;
import com.school.classboard.repository.CourseRepository;
import com.school.classboard.repository.DutyRosterRepository;
import com.school.classboard.repository.HomeworkRepository;
import com.school.classboard.repository.StudentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示数据补充：确保高一(1)~高一(6)班存在，并让每个班的学生记录数与班级人数一致。
 *
 * <p>按班级名称去重，已有数据只补不删，可安全地重复执行。
 * 可通过 classboard.demo.multi-class-enabled=false 关闭。
 */
@Component
@Order(3)
public class DemoClassSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoClassSeeder.class);

    private record DemoClass(int number, String name, String headTeacher, String classroom,
                             String motto, int studentCount) {
    }

    private static final List<DemoClass> DEMO_CLASSES = List.of(
            new DemoClass(1, "高一(1)班", "王建国", "教学楼B-201", "博学笃行，自强不息", 48),
            new DemoClass(2, "高一(2)班", "赵晓梅", "教学楼B-202", "团结奋进，求实创新", 47),
            new DemoClass(3, "高一(3)班", "李明", "教学楼A-301", "志存高远，脚踏实地", 46),
            new DemoClass(4, "高一(4)班", "陈志远", "教学楼B-301", "勤学善思，明辨笃行", 45),
            new DemoClass(5, "高一(5)班", "刘芳", "教学楼B-302", "心怀梦想，脚踏实地", 46),
            new DemoClass(6, "高一(6)班", "孙立军", "教学楼B-303", "厚德载物，知行合一", 44)
    );

    private static final String[][] SUBJECTS = {
            {"语文", "数学", "英语", "物理", "化学", "生物", "体育"},
            {"数学", "英语", "语文", "历史", "地理", "政治", "自习"},
            {"英语", "物理", "数学", "化学", "语文", "信息技术", "自习"}
    };
    private static final String[] START_TIMES = {"08:00", "08:55", "10:00", "10:55", "14:00", "14:55", "19:00"};
    private static final String[] END_TIMES = {"08:45", "09:40", "10:45", "11:40", "14:45", "15:40", "19:45"};

    private static final Map<String, String> TEACHERS = Map.ofEntries(
            Map.entry("语文", "张老师"),
            Map.entry("数学", "李老师"),
            Map.entry("英语", "王老师"),
            Map.entry("物理", "赵老师"),
            Map.entry("化学", "陈老师"),
            Map.entry("生物", "刘老师"),
            Map.entry("体育", "孙老师"),
            Map.entry("历史", "周老师"),
            Map.entry("地理", "吴老师"),
            Map.entry("政治", "郑老师"),
            Map.entry("信息技术", "徐老师"));

    private static final String[] SURNAMES = {
            "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "何", "高", "林", "罗"};
    private static final String[] GIVEN_NAMES = {
            "子涵", "雨桐", "浩然", "欣怡", "泽宇", "思远", "语彤", "梓豪", "若曦", "一鸣",
            "书瑶", "俊杰", "梦琪", "宇航", "佳怡", "博文"};

    private final boolean enabled;
    private final ClassInfoRepository classInfoRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final DutyRosterRepository dutyRepository;
    private final HomeworkRepository homeworkRepository;

    public DemoClassSeeder(@Value("${classboard.demo.multi-class-enabled:true}") boolean enabled,
                           ClassInfoRepository classInfoRepository,
                           CourseRepository courseRepository,
                           StudentRepository studentRepository,
                           DutyRosterRepository dutyRepository,
                           HomeworkRepository homeworkRepository) {
        this.enabled = enabled;
        this.classInfoRepository = classInfoRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.dutyRepository = dutyRepository;
        this.homeworkRepository = homeworkRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        Map<String, ClassInfo> existing = classInfoRepository.findAll().stream()
                .collect(Collectors.toMap(ClassInfo::getName, clazz -> clazz, (a, b) -> a));

        int createdClasses = 0;
        int addedStudents = 0;
        for (DemoClass demo : DEMO_CLASSES) {
            ClassInfo clazz = existing.get(demo.name());
            boolean created = false;
            if (clazz == null) {
                clazz = new ClassInfo();
                clazz.setName(demo.name());
                clazz.setGrade("2025级");
                clazz.setHeadTeacher(demo.headTeacher());
                clazz.setClassroom(demo.classroom());
                clazz.setMotto(demo.motto());
                clazz.setStudentCount(demo.studentCount());
                classInfoRepository.save(clazz);
                seedCourses(clazz.getId(), demo.headTeacher());
                created = true;
                createdClasses++;
            } else if (clazz.getStudentCount() == null || clazz.getStudentCount() < demo.studentCount()) {
                clazz.setStudentCount(demo.studentCount());
                classInfoRepository.save(clazz);
            }

            addedStudents += backfillStudents(clazz, demo);
            seedHomework(clazz);
            ensureDuty(clazz);
        }
        if (createdClasses > 0 || addedStudents > 0) {
            log.info("演示班级数据已补齐：新增 {} 个班，补充 {} 名学生", createdClasses, addedStudents);
        }
    }

    private void seedCourses(Long classId, String headTeacher) {
        for (int day = 1; day <= 5; day++) {
            for (int period = 1; period <= 7; period++) {
                String subject = SUBJECTS[(day - 1) % SUBJECTS.length][period - 1];
                Course course = new Course();
                course.setClassId(classId);
                course.setDayOfWeek(day);
                course.setPeriod(period);
                course.setSubject(subject);
                course.setTeacher("自习".equals(subject) ? headTeacher
                        : TEACHERS.getOrDefault(subject, "任课老师"));
                course.setStartTime(START_TIMES[period - 1]);
                course.setEndTime(END_TIMES[period - 1]);
                courseRepository.save(course);
            }
        }
    }

    /**
     * 把学生记录补齐到班级人数。
     *
     * @return 新补充的学生数
     */
    private int backfillStudents(ClassInfo clazz, DemoClass demo) {
        List<Student> existing = new ArrayList<>(studentRepository.findByClassIdOrderByNameAsc(clazz.getId()));
        int target = demo.studentCount();
        if (existing.size() >= target) {
            return 0;
        }

        Set<String> names = existing.stream().map(Student::getName).collect(Collectors.toCollection(HashSet::new));
        LocalDate today = LocalDate.now();
        int added = 0;

        for (int i = existing.size(); i < target; i++) {
            String cardNo = nextCardNo(demo.number(), i);
            String name = generateName(demo.number(), i, names);

            Student student = new Student();
            student.setClassId(clazz.getId());
            student.setName(name);
            student.setCardNo(cardNo);
            LocalDate birthday = LocalDate.of(2009 + (i % 2), 1 + ((i * 5) % 12), 1 + (i % 27));
            if (i == 0) {
                birthday = LocalDate.of(birthday.getYear(), today.getMonthValue(), today.getDayOfMonth());
            }
            student.setBirthday(birthday);
            student.setEnabled(true);
            studentRepository.save(student);
            added++;
        }
        return added;
    }

    private String nextCardNo(int classNumber, int index) {
        String cardNo = String.format("2025%02d01%02d", classNumber, index + 1);
        int bump = 0;
        while (studentRepository.existsByCardNo(cardNo)) {
            bump++;
            cardNo = String.format("2025%02d9%03d", classNumber, index + 1 + bump);
        }
        return cardNo;
    }

    private String generateName(int classNumber, int index, Set<String> usedNames) {
        int seed = (classNumber - 1) * 10 + index;
        String name = SURNAMES[seed % SURNAMES.length] + GIVEN_NAMES[(seed * 3) % GIVEN_NAMES.length];
        int bump = 0;
        while (usedNames.contains(name) && bump < 60) {
            bump++;
            name = SURNAMES[(seed + bump) % SURNAMES.length]
                    + GIVEN_NAMES[(seed * 3 + bump) % GIVEN_NAMES.length];
        }
        if (usedNames.contains(name)) {
            name = name + (index + 1);
        }
        usedNames.add(name);
        return name;
    }

    private void seedHomework(ClassInfo clazz) {
        LocalDate today = LocalDate.now();
        if (homeworkRepository.countByClassIdAndDueDate(clazz.getId(), today) > 0) {
            return;
        }
        String[][] items = {
                {"语文", "背诵《劝学》并完成练习册第 12 页", "张老师"},
                {"数学", "完成第三章函数习题 3.1–3.5", "李老师"},
                {"英语", "背诵 Unit 3 单词，完成两篇阅读理解", "王老师"}
        };
        for (String[] item : items) {
            Homework homework = new Homework();
            homework.setClassId(clazz.getId());
            homework.setSubject(item[0]);
            homework.setContent(item[1]);
            homework.setTeacher(item[2]);
            homework.setDueDate(today);
            homework.setCreatedAt(LocalDateTime.now());
            homeworkRepository.save(homework);
        }
    }

    private void ensureDuty(ClassInfo clazz) {
        if (!dutyRepository.findByClassIdOrderByDayOfWeekAsc(clazz.getId()).isEmpty()) {
            return;
        }
        List<Student> students = studentRepository.findByClassIdOrderByNameAsc(clazz.getId());
        String[] names = students.stream().map(Student::getName).limit(12).toArray(String[]::new);
        String[] groups = {
                join(names, 0, 4),
                join(names, 4, 8),
                join(names, 8, 12)
        };
        for (int day = 1; day <= 5; day++) {
            DutyRoster roster = new DutyRoster();
            roster.setClassId(clazz.getId());
            roster.setDayOfWeek(day);
            roster.setStudents(groups[(day - 1) % groups.length]);
            roster.setTask(day % 2 == 1 ? "扫地、拖地、擦黑板" : "倒垃圾、整理讲台、关灯关窗");
            dutyRepository.save(roster);
        }
    }

    private String join(String[] names, int from, int to) {
        return Arrays.stream(names, from, Math.min(to, names.length))
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining("、"));
    }
}
