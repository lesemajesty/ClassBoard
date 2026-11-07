package com.school.classboard.service;

import com.school.classboard.domain.DutyRoster;
import com.school.classboard.repository.DutyRosterRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 班级值日表。
 */
@Service
@Transactional(readOnly = true)
public class DutyService {

    private final DutyRosterRepository repository;

    public DutyService(DutyRosterRepository repository) {
        this.repository = repository;
    }

    public List<DutyRoster> forClass(Long classId) {
        return repository.findByClassIdOrderByDayOfWeekAsc(classId);
    }

    public Optional<DutyRoster> todayForClass(Long classId) {
        return repository.findByClassIdAndDayOfWeek(classId, LocalDate.now().getDayOfWeek().getValue());
    }

    @Transactional
    public DutyRoster save(Long classId, int dayOfWeek, String students, String task) {
        DutyRoster roster = repository.findByClassIdAndDayOfWeek(classId, dayOfWeek)
                .orElseGet(() -> {
                    DutyRoster created = new DutyRoster();
                    created.setClassId(classId);
                    created.setDayOfWeek(dayOfWeek);
                    return created;
                });
        roster.setStudents(students);
        roster.setTask(task);
        return repository.save(roster);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
