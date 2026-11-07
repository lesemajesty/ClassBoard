package com.school.classboard.repository;

import com.school.classboard.domain.DutyRoster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DutyRosterRepository extends JpaRepository<DutyRoster, Long> {

    List<DutyRoster> findByClassIdOrderByDayOfWeekAsc(Long classId);

    Optional<DutyRoster> findByClassIdAndDayOfWeek(Long classId, Integer dayOfWeek);
}
