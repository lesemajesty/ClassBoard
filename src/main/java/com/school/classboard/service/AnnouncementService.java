package com.school.classboard.service;

import com.school.classboard.domain.Announcement;
import com.school.classboard.repository.AnnouncementRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知公告业务逻辑。
 */
@Service
@Transactional(readOnly = true)
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository repository;

    public AnnouncementService(AnnouncementRepository repository) {
        this.repository = repository;
    }

    /** 某班级可见的公告：全校公告（classId 为空）+ 本班公告，已过期的不会返回 */
    public List<Announcement> findForClass(Long classId) {
        return repository.findVisibleToClass(classId, LocalDateTime.now());
    }

    public List<Announcement> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Announcement save(Announcement announcement) {
        LocalDateTime now = LocalDateTime.now();
        if (announcement.getId() == null && announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(now);
        }
        // 考试/活动类公告默认在事件结束后的第二天自动过期
        if (announcement.getEventDate() != null && announcement.getExpireAt() == null) {
            announcement.setExpireAt(announcement.getEventDate().plusDays(1).atStartOfDay());
        }
        if (announcement.getExpireAt() != null && !announcement.getExpireAt().isAfter(now)) {
            announcement.setActive(false);
        }
        return repository.save(announcement);
    }

    /** 定时把超过有效期的公告下线，避免旧通知一直挂在班牌上 */
    @Scheduled(initialDelayString = "${classboard.announcement.cleanup-initial-delay-ms:15000}",
            fixedDelayString = "${classboard.announcement.cleanup-ms:3600000}")
    @Transactional
    public void scheduledArchiveExpired() {
        archiveExpired();
    }

    @Transactional
    public int archiveExpired() {
        int archived = repository.archiveExpired(LocalDateTime.now());
        if (archived > 0) {
            log.info("已自动回收 {} 条过期公告", archived);
        }
        return archived;
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
