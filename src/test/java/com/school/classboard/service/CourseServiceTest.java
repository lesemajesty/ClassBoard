package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.Course;
import com.school.classboard.repository.CourseRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private CourseService service;

    @Test
    void saveForSlotCreatesNewCourseWhenSlotIsEmpty() {
        Course input = course(2L, 1, 3);
        input.setSubject("数学");
        input.setTeacher("张老师");
        when(repository.findByClassIdAndDayOfWeekAndPeriod(2L, 1, 3)).thenReturn(Optional.empty());
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course saved = service.saveForSlot(input);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        Course persisted = captor.getValue();
        assertSame(persisted, saved);
        assertNull(persisted.getId());
        assertEquals(2L, persisted.getClassId());
        assertEquals(1, persisted.getDayOfWeek().intValue());
        assertEquals(3, persisted.getPeriod().intValue());
        assertEquals("数学", persisted.getSubject());
        assertEquals("张老师", persisted.getTeacher());
    }

    @Test
    void saveForSlotOverwritesExistingCourseInPlace() {
        Course existing = course(2L, 1, 3);
        existing.setId(99L);
        existing.setSubject("数学");
        Course input = course(2L, 1, 3);
        input.setSubject("英语");
        input.setTeacher("李老师");
        input.setStartTime("08:00");
        input.setEndTime("08:45");

        when(repository.findByClassIdAndDayOfWeekAndPeriod(2L, 1, 3)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Course saved = service.saveForSlot(input);

        assertSame(existing, saved);
        assertEquals(99L, saved.getId());
        assertEquals("英语", existing.getSubject());
        assertEquals("李老师", existing.getTeacher());
        assertEquals("08:00", existing.getStartTime());
        assertEquals("08:45", existing.getEndTime());
    }

    @Test
    void saveForSlotRejectsMissingClass() {
        Course input = new Course();
        input.setDayOfWeek(1);
        input.setPeriod(1);

        assertThrows(IllegalArgumentException.class, () -> service.saveForSlot(input));
        verifyNoInteractions(repository);
    }

    @Test
    void saveForSlotRejectsMissingDayOrPeriod() {
        Course noDay = new Course();
        noDay.setClassId(1L);
        noDay.setPeriod(1);
        assertThrows(IllegalArgumentException.class, () -> service.saveForSlot(noDay));

        Course noPeriod = new Course();
        noPeriod.setClassId(1L);
        noPeriod.setDayOfWeek(2);
        assertThrows(IllegalArgumentException.class, () -> service.saveForSlot(noPeriod));

        verifyNoInteractions(repository);
    }

    private Course course(Long classId, int dayOfWeek, int period) {
        Course course = new Course();
        course.setClassId(classId);
        course.setDayOfWeek(dayOfWeek);
        course.setPeriod(period);
        return course;
    }
}
