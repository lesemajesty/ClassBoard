package com.school.classboard.service;

import com.school.classboard.domain.Homework;
import com.school.classboard.repository.HomeworkRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 班级作业。
 */
@Service
@Transactional(readOnly = true)
public class HomeworkService {

    private final HomeworkRepository repository;

    public HomeworkService(HomeworkRepository repository) {
        this.repository = repository;
    }

    public List<Homework> forClass(Long classId) {
        return classId == null ? List.of() : repository.findByClassIdOrderByDueDateDescCreatedAtDesc(classId);
    }

    /** 今天及以后需要完成的作业 */
    public List<Homework> upcoming(Long classId) {
        return classId == null ? List.of()
                : repository.findByClassIdAndDueDateGreaterThanEqualOrderByDueDateAscCreatedAtAsc(
                        classId, LocalDate.now());
    }

    public List<Homework> forDate(Long classId, LocalDate date) {
        return classId == null || date == null ? List.of()
                : repository.findByClassIdAndDueDateOrderBySubjectAsc(classId, date);
    }

    @Transactional
    public Homework save(Homework homework) {
        if (homework.getCreatedAt() == null) {
            homework.setCreatedAt(LocalDateTime.now());
        }
        if (homework.getDueDate() == null) {
            homework.setDueDate(LocalDate.now());
        }
        return repository.save(homework);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
