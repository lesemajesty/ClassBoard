package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.Student;
import com.school.classboard.repository.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @InjectMocks
    private StudentService service;

    @Test
    void cardIsFreeWhenNoOneUsesIt() {
        when(repository.existsByCardNo("2025030101")).thenReturn(false);

        assertFalse(service.isCardTaken("2025030101", null));
    }

    @Test
    void cardIsTakenByAnotherStudent() {
        when(repository.existsByCardNoAndIdNot("2025030101", 1L)).thenReturn(true);

        assertTrue(service.isCardTaken(" 2025030101 ", 1L));
    }

    @Test
    void cardIsFreeForTheStudentThatOwnsIt() {
        when(repository.existsByCardNoAndIdNot("2025030101", 1L)).thenReturn(false);

        assertFalse(service.isCardTaken("2025030101", 1L));
    }

    @Test
    void birthdaysTodayOnlyMatchesMonthAndDay() {
        LocalDate today = LocalDate.now();
        Student birthdayStudent = student(1L, "王雨桐", LocalDate.of(2009, today.getMonthValue(), today.getDayOfMonth()));
        Student otherStudent = student(2L, "李泽宇", today.minusDays(1));
        when(repository.findByClassIdOrderByNameAsc(10L)).thenReturn(List.of(birthdayStudent, otherStudent));

        List<Student> result = service.birthdaysToday(10L);

        assertEquals(1, result.size());
        assertEquals("王雨桐", result.get(0).getName());
    }

    private Student student(Long id, String name, LocalDate birthday) {
        Student student = new Student();
        student.setId(id);
        student.setClassId(10L);
        student.setName(name);
        student.setCardNo("C" + id);
        student.setBirthday(birthday);
        student.setEnabled(true);
        return student;
    }
}
