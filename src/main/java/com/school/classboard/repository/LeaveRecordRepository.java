package com.school.classboard.repository;

import com.school.classboard.domain.LeaveRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRecordRepository extends JpaRepository<LeaveRecord, Long> {

    List<LeaveRecord> findByClassIdOrderByStartDateDescCreatedAtDesc(Long classId);

    List<LeaveRecord> findByClassIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long classId, LocalDate start, LocalDate end);

    boolean existsByStudentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long studentId, LocalDate start, LocalDate end);
}
