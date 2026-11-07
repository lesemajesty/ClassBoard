package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.LeaveRecord;
import com.school.classboard.domain.Student;
import com.school.classboard.repository.LeaveRecordRepository;
import com.school.classboard.repository.StudentRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRecordRepository repository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private LeaveService service;

    @Test
    void createSnapshotsStudentNameAndApproves() {
        Student student = new Student();
        student.setId(7L);
        student.setClassId(2L);
        student.setName("王雨桐");
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));
        when(repository.save(any(LeaveRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRecord record = service.create(2L, 7L, "病假",
                LocalDate.now(), LocalDate.now().plusDays(1), "发烧");

        assertEquals("王雨桐", record.getStudentName());
        assertEquals("病假", record.getType());
        assertEquals(2L, record.getClassId());
        assertTrue(record.isApproved());
    }

    @Test
    void createRejectsStudentFromAnotherClass() {
        Student student = new Student();
        student.setId(7L);
        student.setClassId(99L);
        student.setName("王雨桐");
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(2L, 7L, "事假", LocalDate.now(), LocalDate.now(), ""));
    }

    @Test
    void createRejectsInvalidDateRange() {
        Student student = new Student();
        student.setId(7L);
        student.setClassId(2L);
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(2L, 7L, "事假",
                        LocalDate.now(), LocalDate.now().minusDays(1), ""));
    }

    @Test
    void isOnLeaveDelegatesToRepository() {
        LocalDate today = LocalDate.now();
        when(repository.existsByStudentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(7L, today, today))
                .thenReturn(true);

        assertTrue(service.isOnLeave(7L, today));
    }

    @Test
    void todayForClassReturnsEmptyWithoutClass() {
        assertTrue(service.todayForClass(null).isEmpty());
    }
}
