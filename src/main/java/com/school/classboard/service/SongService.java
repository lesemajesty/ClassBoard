package com.school.classboard.service;

import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import com.school.classboard.repository.SongRequestRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 广播站点歌：学生提交申请，广播室审核后进入今日歌单。
 */
@Service
@Transactional(readOnly = true)
public class SongService {

    private final SongRequestRepository repository;

    public SongService(SongRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SongRequest submit(Long classId, String song, String artist, String requester, String reason) {
        SongRequest request = new SongRequest();
        request.setClassId(classId);
        request.setSong(song == null ? null : song.trim());
        request.setArtist(artist == null ? null : artist.trim());
        request.setRequester(requester == null ? null : requester.trim());
        request.setReason(reason == null ? null : reason.trim());
        request.setStatus(SongStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return repository.save(request);
    }

    /** 班牌歌单：已通过 + 已播放 */
    public List<SongRequest> playlist() {
        return repository.findByStatusInOrderByCreatedAtDesc(List.of(SongStatus.APPROVED, SongStatus.PLAYED))
                .stream().limit(8).toList();
    }

    public List<SongRequest> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /** 本班点歌记录：包含待审核、已通过、已播放和已驳回，用于给学生留存审核通知 */
    public List<SongRequest> forClass(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return repository.findByClassIdOrderByCreatedAtDesc(classId);
    }

    public long pendingCount() {
        return repository.findByStatusOrderByCreatedAtDesc(SongStatus.PENDING).size();
    }

    /** 驳回时保留原因，学生下次打开点歌页仍能看到这条通知 */
    @Transactional
    public void reject(Long id, String reason) {
        repository.findById(id).ifPresent(request -> {
            request.setStatus(SongStatus.REJECTED);
            request.setRejectReason(reason == null || reason.isBlank() ? null : reason.trim());
            request.setReviewedAt(LocalDateTime.now());
            request.setRejectReadAt(null);
            request.setPlayedAt(null);
        });
    }

    /** 学生点击「已读确认」后记录确认时间，通知仍保留在本班点歌记录中 */
    @Transactional
    public void markRejectRead(Long id) {
        repository.findById(id).ifPresent(request -> {
            if (request.getStatus() == SongStatus.REJECTED) {
                request.setRejectReadAt(LocalDateTime.now());
            }
        });
    }

    @Transactional
    public void updateStatus(Long id, SongStatus status) {
        repository.findById(id).ifPresent(request -> {
            request.setStatus(status);
            request.setPlayedAt(status == SongStatus.PLAYED ? LocalDateTime.now() : null);
            if (status != SongStatus.PENDING) {
                request.setReviewedAt(LocalDateTime.now());
            }
            if (status != SongStatus.REJECTED) {
                request.setRejectReason(null);
                request.setRejectReadAt(null);
            }
        });
    }
}
