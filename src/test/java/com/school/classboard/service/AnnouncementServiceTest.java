package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.Announcement;
import com.school.classboard.repository.AnnouncementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository repository;

    @InjectMocks
    private AnnouncementService service;

    @Test
    void findForClassReturnsVisibleAnnouncementsFromQuery() {
        Announcement schoolWide = announcement("全校通知", null);
        Announcement classOnly = announcement("本班通知", 3L);
        when(repository.findVisibleToClass(eq(3L), any(LocalDateTime.class)))
                .thenReturn(List.of(schoolWide, classOnly));

        List<Announcement> result = service.findForClass(3L);

        assertEquals(List.of(schoolWide, classOnly), result);
        verify(repository).findVisibleToClass(eq(3L), any(LocalDateTime.class));
    }

    @Test
    void saveStampsNewAnnouncementWithCurrentTime() {
        Announcement announcement = announcement("新公告", null);
        when(repository.save(announcement)).thenReturn(announcement);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        service.save(announcement);

        assertNotNull(announcement.getPublishedAt());
        assertTrue(announcement.getPublishedAt().isAfter(before));
    }

    @Test
    void saveKeepsExplicitPublishTime() {
        Announcement announcement = announcement("补录公告", null);
        LocalDateTime published = LocalDateTime.of(2026, 1, 2, 8, 30);
        announcement.setPublishedAt(published);
        when(repository.save(announcement)).thenReturn(announcement);

        service.save(announcement);

        assertEquals(published, announcement.getPublishedAt());
    }

    @Test
    void eventAnnouncementExpiresAfterEventDate() {
        Announcement announcement = announcement("期中考试", null);
        LocalDate eventDate = LocalDate.now().plusDays(5);
        announcement.setEventDate(eventDate);
        when(repository.save(announcement)).thenReturn(announcement);

        service.save(announcement);

        assertEquals(eventDate.plusDays(1).atStartOfDay(), announcement.getExpireAt());
    }

    @Test
    void savingPastExpiryMarksAnnouncementInactive() {
        Announcement announcement = announcement("过期通知", null);
        announcement.setExpireAt(LocalDateTime.now().minusDays(1));
        when(repository.save(announcement)).thenReturn(announcement);

        service.save(announcement);

        assertFalse(announcement.isActive());
    }

    @Test
    void archiveExpiredDelegatesToRepository() {
        when(repository.archiveExpired(any(LocalDateTime.class))).thenReturn(2);

        assertEquals(2, service.archiveExpired());
        verify(repository).archiveExpired(any(LocalDateTime.class));
    }

    @Test
    void deleteDelegatesToRepository() {
        service.delete(5L);

        verify(repository).deleteById(5L);
    }

    private Announcement announcement(String title, Long classId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setClassId(classId);
        return announcement;
    }
}
