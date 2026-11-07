package com.school.classboard.service;

import com.school.classboard.domain.LeaveRecord;
import com.school.classboard.domain.Student;
import com.school.classboard.repository.LeaveRecordRepository;
import com.school.classboard.repository.StudentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学生请假登记。
 */
@Service
@Transactional(readOnly = true)
public class LeaveService {

    private final LeaveRecordRepository repository;
    private final StudentRepository studentRepository;

    public LeaveService(LeaveRecordRepository repository, StudentRepository studentRepository) {
        this.repository = repository;
        this.studentRepository = studentRepository;
    }

    /** 今天处于请假中的记录 */
    public List<LeaveRecord> todayForClass(Long classId) {
        if (classId == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return repository.findByClassIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                classId, today, today);
    }

    public List<LeaveRecord> forClass(Long classId) {
        return classId == null ? List.of() : repository.findByClassIdOrderByStartDateDescCreatedAtDesc(classId);
    }

    public boolean isOnLeave(Long studentId, LocalDate date) {
        return studentId != null && date != null
                && repository.existsByStudentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        studentId, date, date);
    }

    @Transactional
    public LeaveRecord create(Long classId, Long studentId, String type,
                              LocalDate startDate, LocalDate endDate, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (!student.getClassId().equals(classId)) {
            throw new IllegalArgumentException("学生不属于当前班级");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("请假日期不正确");
        }
        LeaveRecord record = new LeaveRecord();
        record.setClassId(classId);
        record.setStudentId(student.getId());
        record.setStudentName(student.getName());
        record.setType(type == null || type.isBlank() ? "事假" : type);
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        record.setReason(reason);
        record.setApproved(true);
        record.setCreatedAt(LocalDateTime.now());
        return repository.save(record);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
