package com.school.classboard.repository;

import com.school.classboard.domain.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /** 某班级整周课表 */
    List<Course> findByClassIdOrderByDayOfWeekAscPeriodAsc(Long classId);

    /** 某班级某天的课程 */
    List<Course> findByClassIdAndDayOfWeekOrderByPeriodAsc(Long classId, Integer dayOfWeek);

    /** 定位某一格（班级 + 星期 + 节次），用于新增/覆盖课表 */
    Optional<Course> findByClassIdAndDayOfWeekAndPeriod(Long classId, Integer dayOfWeek, Integer period);

    /** 删除班级时一并清理该班课表 */
    @Modifying
    @Query("delete from Course c where c.classId = :classId")
    int deleteByClassId(@Param("classId") Long classId);
}
