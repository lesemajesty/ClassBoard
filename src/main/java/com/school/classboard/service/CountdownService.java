package com.school.classboard.service;

import com.school.classboard.domain.CountdownEvent;
import com.school.classboard.repository.CountdownEventRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 考试 / 活动倒计时。
 */
@Service
@Transactional(readOnly = true)
public class CountdownService {

    private final CountdownEventRepository repository;

    public CountdownService(CountdownEventRepository repository) {
        this.repository = repository;
    }

    /** 最近一个尚未到来的事件 */
    public Optional<CountdownEvent> nearest() {
        return repository.findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate.now())
                .stream().findFirst();
    }

    public List<CountdownEvent> findAll() {
        return repository.findAllByOrderByEventDateAsc();
    }

    public long daysUntil(CountdownEvent event) {
        return ChronoUnit.DAYS.between(LocalDate.now(), event.getEventDate());
    }

    @Transactional
    public CountdownEvent save(CountdownEvent event) {
        return repository.save(event);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
