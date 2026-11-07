package com.school.classboard.service;

import com.school.classboard.domain.Course;
import com.school.classboard.repository.CourseRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 课程表业务逻辑。
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository repository;

    public CourseService(CourseRepository repository) {
        this.repository = repository;
    }

    public List<Course> findWeekly(Long classId) {
        return repository.findByClassIdOrderByDayOfWeekAscPeriodAsc(classId);
    }

    public List<Course> findToday(Long classId) {
        return repository.findByClassIdAndDayOfWeekOrderByPeriodAsc(classId, currentDayOfWeek());
    }

    public Optional<Course> findSlot(Long classId, int dayOfWeek, int period) {
        return repository.findByClassIdAndDayOfWeekAndPeriod(classId, dayOfWeek, period);
    }

    /** 返回 1=周一 ... 7=周日 */
    public static int currentDayOfWeek() {
        return LocalDate.now().getDayOfWeek().getValue();
    }

    public static String dayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "";
        };
    }

    /**
     * 保存某一格课程：同一班级、星期、节次已存在时覆盖原记录，避免产生重复行。
     */
    @Transactional
    public Course saveForSlot(Course course) {
        if (course.getClassId() == null) {
            throw new IllegalArgumentException("班级不能为空");
        }
        if (course.getDayOfWeek() == null || course.getPeriod() == null) {
            throw new IllegalArgumentException("星期和节次不能为空");
        }
        Course target = repository
                .findByClassIdAndDayOfWeekAndPeriod(course.getClassId(), course.getDayOfWeek(), course.getPeriod())
                .orElseGet(() -> {
                    Course created = new Course();
                    created.setClassId(course.getClassId());
                    created.setDayOfWeek(course.getDayOfWeek());
                    created.setPeriod(course.getPeriod());
                    return created;
                });
        target.setSubject(course.getSubject());
        target.setTeacher(course.getTeacher());
        target.setStartTime(course.getStartTime());
        target.setEndTime(course.getEndTime());
        return repository.save(target);
    }

    @Transactional
    public Course save(Course course) {
        return repository.save(course);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
