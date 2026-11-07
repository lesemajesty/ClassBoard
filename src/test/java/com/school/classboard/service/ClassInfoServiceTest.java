package com.school.classboard.service;

import static org.mockito.Mockito.inOrder;

import com.school.classboard.repository.AnnouncementRepository;
import com.school.classboard.repository.ClassInfoRepository;
import com.school.classboard.repository.CourseRepository;
import com.school.classboard.repository.TerminalDeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassInfoServiceTest {

    @Mock
    private ClassInfoRepository repository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private TerminalDeviceRepository terminalDeviceRepository;

    @InjectMocks
    private ClassInfoService service;

    @Test
    void deleteCleansRelatedDataBeforeRemovingClass() {
        service.delete(7L);

        InOrder order = inOrder(courseRepository, announcementRepository, terminalDeviceRepository, repository);
        order.verify(courseRepository).deleteByClassId(7L);
        order.verify(announcementRepository).deleteByClassId(7L);
        order.verify(terminalDeviceRepository).unbindClass(7L);
        order.verify(repository).deleteById(7L);
    }
}
