package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.CheckInRecord;
import com.school.classboard.domain.CheckInType;
import com.school.classboard.domain.LeaveRecord;
import com.school.classboard.domain.Student;
import com.school.classboard.repository.CheckInRecordRepository;
import com.school.classboard.repository.StudentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CheckInRecordRepository recordRepository;

    @Mock
    private LeaveService leaveService;

    private CheckInService service;

    @BeforeEach
    void setUp() {
        service = new CheckInService(studentRepository, recordRepository, leaveService, "07:30");
    }

    @Test
    void unknownCardIsRejected() {
        when(studentRepository.findByCardNo("404")).thenReturn(Optional.empty());

        CheckInService.Result result = service.checkIn(10L, "404");

        assertFalse(result.ok());
        assertEquals("卡号未登记", result.title());
        verifyNoInteractions(recordRepository);
    }

    @Test
    void disabledCardIsRejected() {
        when(studentRepository.findByCardNo("C1")).thenReturn(Optional.of(student(1L, 10L, "王雨桐", false)));

        CheckInService.Result result = service.checkIn(10L, "C1");

        assertFalse(result.ok());
        assertEquals("校园卡已停用", result.title());
        verifyNoInteractions(recordRepository);
    }

    @Test
    void cardOfAnotherClassIsRejected() {
        when(studentRepository.findByCardNo("C1")).thenReturn(Optional.of(student(1L, 99L, "王雨桐", true)));

        CheckInService.Result result = service.checkIn(10L, "C1");

        assertFalse(result.ok());
        assertEquals("不是本班学生", result.title());
        verifyNoInteractions(recordRepository);
    }

    @Test
    void firstSwipeAfterDeadlineIsMarkedLate() {
        CheckInService alwaysLate = new CheckInService(studentRepository, recordRepository, leaveService, "00:00");
        when(studentRepository.findByCardNo("C1")).thenReturn(Optional.of(student(1L, 10L, "王雨桐", true)));
        when(recordRepository.findTopByStudentIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(1L), any()))
                .thenReturn(Optional.empty());

        CheckInService.Result result = alwaysLate.checkIn(10L, "C1");

        assertTrue(result.ok());
        assertEquals("迟到签到", result.title());
        assertTrue(result.record().isLate());
        assertEquals(CheckInType.IN, result.record().getType());
        verify(recordRepository).save(any(CheckInRecord.class));
    }

    @Test
    void firstSwipeBeforeDeadlineIsNormal() {
        CheckInService lenient = new CheckInService(studentRepository, recordRepository, leaveService, "23:59");
        when(studentRepository.findByCardNo("C1")).thenReturn(Optional.of(student(1L, 10L, "王雨桐", true)));
        when(recordRepository.findTopByStudentIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(1L), any()))
                .thenReturn(Optional.empty());

        CheckInService.Result result = lenient.checkIn(10L, "C1");

        assertTrue(result.ok());
        assertEquals("签到成功", result.title());
        assertFalse(result.record().isLate());
    }

    @Test
    void secondSwipeCreatesCheckOut() {
        when(studentRepository.findByCardNo("C1")).thenReturn(Optional.of(student(1L, 10L, "王雨桐", true)));
        when(recordRepository.findTopByStudentIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(1L), any()))
                .thenReturn(Optional.of(record(1L, CheckInType.IN)));

        CheckInService.Result result = service.checkIn(10L, "C1");

        assertTrue(result.ok());
        assertEquals("签退成功", result.title());
        assertEquals(CheckInType.OUT, result.record().getType());
    }

    @Test
    void thirdSwipeIsRejected() {
        when(studentRepository.findByCardNo("C1")).thenReturn(Optional.of(student(1L, 10L, "王雨桐", true)));
        when(recordRepository.findTopByStudentIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(1L), any()))
                .thenReturn(Optional.of(record(1L, CheckInType.OUT)));

        CheckInService.Result result = service.checkIn(10L, "C1");

        assertFalse(result.ok());
        assertEquals("今日已完成签到签退", result.title());
    }

    @Test
    void statsCountPresentLateAndLeft() {
        when(studentRepository.findByClassIdAndEnabledTrueOrderByNameAsc(10L))
                .thenReturn(List.of(student(1L, 10L, "王雨桐", true), student(2L, 10L, "李泽宇", true)));
        CheckInRecord lateIn = record(1L, CheckInType.IN);
        lateIn.setLate(true);
        when(recordRepository.findByClassIdAndCheckedAtBetweenOrderByCheckedAtDesc(eq(10L), any(), any()))
                .thenReturn(List.of(lateIn, record(1L, CheckInType.OUT), record(2L, CheckInType.IN)));

        CheckInService.Stats stats = service.stats(10L);

        assertEquals(2, stats.expected());
        assertEquals(2, stats.present());
        assertEquals(1, stats.late());
        assertEquals(1, stats.left());
        assertEquals(0, stats.leave());
    }

    @Test
    void studentOnLeaveCannotCheckIn() {
        when(studentRepository.findByCardNo("C1"))
                .thenReturn(Optional.of(student(1L, 10L, "王雨桐", true)));
        when(leaveService.isOnLeave(eq(1L), any())).thenReturn(true);

        CheckInService.Result result = service.checkIn(10L, "C1");

        assertFalse(result.ok());
        assertEquals("今日请假", result.title());
        verifyNoInteractions(recordRepository);
    }

    @Test
    void statsSubtractsLeaveFromExpected() {
        when(studentRepository.findByClassIdAndEnabledTrueOrderByNameAsc(10L))
                .thenReturn(List.of(student(1L, 10L, "王雨桐", true), student(2L, 10L, "李泽宇", true)));

        LeaveRecord leave = new LeaveRecord();
        leave.setStudentId(1L);
        when(leaveService.todayForClass(10L)).thenReturn(List.of(leave));
        when(recordRepository.findByClassIdAndCheckedAtBetweenOrderByCheckedAtDesc(eq(10L), any(), any()))
                .thenReturn(List.of(record(2L, CheckInType.IN)));

        CheckInService.Stats stats = service.stats(10L);

        assertEquals(1, stats.expected());
        assertEquals(1, stats.present());
        assertEquals(1, stats.leave());
    }

    private Student student(Long id, Long classId, String name, boolean enabled) {
        Student student = new Student();
        student.setId(id);
        student.setClassId(classId);
        student.setName(name);
        student.setCardNo("C" + id);
        student.setEnabled(enabled);
        return student;
    }

    private CheckInRecord record(Long studentId, CheckInType type) {
        CheckInRecord record = new CheckInRecord();
        record.setStudentId(studentId);
        record.setStudentName("学生" + studentId);
        record.setType(type);
        record.setCheckedAt(java.time.LocalDateTime.now());
        return record;
    }
}
