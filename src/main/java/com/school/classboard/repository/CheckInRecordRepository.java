package com.school.classboard.repository;

import com.school.classboard.domain.CheckInRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, Long> {

    List<CheckInRecord> findByClassIdAndCheckedAtBetweenOrderByCheckedAtDesc(
            Long classId, LocalDateTime from, LocalDateTime to);

    Optional<CheckInRecord> findTopByStudentIdAndCheckedAtAfterOrderByCheckedAtDesc(
            Long studentId, LocalDateTime from);

    List<CheckInRecord> findByCheckedAtBetweenOrderByCheckedAtDesc(LocalDateTime from, LocalDateTime to);
}
