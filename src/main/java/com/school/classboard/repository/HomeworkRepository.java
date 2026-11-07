package com.school.classboard.repository;

import com.school.classboard.domain.Homework;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    List<Homework> findByClassIdOrderByDueDateDescCreatedAtDesc(Long classId);

    List<Homework> findByClassIdAndDueDateGreaterThanEqualOrderByDueDateAscCreatedAtAsc(
            Long classId, LocalDate date);

    List<Homework> findByClassIdAndDueDateOrderBySubjectAsc(Long classId, LocalDate date);

    long countByClassIdAndDueDate(Long classId, LocalDate date);
}
