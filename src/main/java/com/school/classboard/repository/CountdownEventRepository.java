package com.school.classboard.repository;

import com.school.classboard.domain.CountdownEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountdownEventRepository extends JpaRepository<CountdownEvent, Long> {

    List<CountdownEvent> findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate date);

    List<CountdownEvent> findAllByOrderByEventDateAsc();
}
