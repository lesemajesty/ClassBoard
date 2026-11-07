package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.Homework;
import com.school.classboard.repository.HomeworkRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeworkServiceTest {

    @Mock
    private HomeworkRepository repository;

    @InjectMocks
    private HomeworkService service;

    @Test
    void saveFillsCreatedAt() {
        Homework homework = new Homework();
        homework.setClassId(1L);
        homework.setSubject("数学");
        homework.setDueDate(LocalDate.now());
        when(repository.save(any(Homework.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Homework saved = service.save(homework);

        assertNotNull(saved.getCreatedAt());
        assertEquals(LocalDate.now(), saved.getDueDate());
    }

    @Test
    void saveDefaultsDueDateToToday() {
        Homework homework = new Homework();
        homework.setClassId(1L);
        homework.setSubject("英语");
        when(repository.save(any(Homework.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Homework saved = service.save(homework);

        assertEquals(LocalDate.now(), saved.getDueDate());
    }
}
