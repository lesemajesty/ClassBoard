package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import com.school.classboard.repository.SongRequestRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRequestRepository repository;

    @InjectMocks
    private SongService service;

    @Test
    void submitTrimsFieldsAndStartsPending() {
        when(repository.save(any(SongRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SongRequest request = service.submit(1L, " 夜空中最亮的星 ", " 逃跑计划 ", " 王雨桐 ", " 给备考的大家 ");

        assertEquals("夜空中最亮的星", request.getSong());
        assertEquals("逃跑计划", request.getArtist());
        assertEquals("王雨桐", request.getRequester());
        assertEquals("给备考的大家", request.getReason());
        assertEquals(SongStatus.PENDING, request.getStatus());
        assertEquals(1L, request.getClassId());
        assertNotNull(request.getCreatedAt());
    }

    @Test
    void markingPlayedStampsPlayedAt() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.APPROVED);
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.updateStatus(5L, SongStatus.PLAYED);

        assertEquals(SongStatus.PLAYED, request.getStatus());
        assertNotNull(request.getPlayedAt());
    }

    @Test
    void approvingClearsPreviousPlayedAt() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.PLAYED);
        request.setPlayedAt(LocalDateTime.now().minusHours(1));
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.updateStatus(5L, SongStatus.APPROVED);

        assertEquals(SongStatus.APPROVED, request.getStatus());
        assertNull(request.getPlayedAt());
    }

    @Test
    void rejectingKeepsReasonAndReviewTime() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.PENDING);
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.reject(5L, " 版权原因 ");

        assertEquals(SongStatus.REJECTED, request.getStatus());
        assertEquals("版权原因", request.getRejectReason());
        assertNotNull(request.getReviewedAt());
        assertNull(request.getPlayedAt());
    }

    @Test
    void rejectingWithoutReasonStillKeepsNotification() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.PENDING);
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.reject(5L, "  ");

        assertEquals(SongStatus.REJECTED, request.getStatus());
        assertNull(request.getRejectReason());
        assertNotNull(request.getReviewedAt());
    }

    @Test
    void approvingClearsRejectReason() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.REJECTED);
        request.setRejectReason("不适合");
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.updateStatus(5L, SongStatus.APPROVED);

        assertEquals(SongStatus.APPROVED, request.getStatus());
        assertNull(request.getRejectReason());
    }

    @Test
    void forClassReturnsAllStatuses() {
        SongRequest request = new SongRequest();
        request.setSong("起风了");
        when(repository.findByClassIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(request));

        List<SongRequest> result = service.forClass(1L);

        assertEquals(1, result.size());
        assertEquals("起风了", result.get(0).getSong());
    }

    @Test
    void markingRejectedNotificationReadStampsTime() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.REJECTED);
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.markRejectRead(5L);

        assertNotNull(request.getRejectReadAt());
    }

    @Test
    void nonRejectedRequestCannotBeMarkedRead() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.PENDING);
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.markRejectRead(5L);

        assertNull(request.getRejectReadAt());
    }

    @Test
    void rejectingAgainClearsPreviousReadState() {
        SongRequest request = new SongRequest();
        request.setId(5L);
        request.setStatus(SongStatus.REJECTED);
        request.setRejectReadAt(LocalDateTime.now().minusHours(1));
        when(repository.findById(5L)).thenReturn(Optional.of(request));

        service.reject(5L, "重新审核");

        assertNull(request.getRejectReadAt());
        assertEquals("重新审核", request.getRejectReason());
    }
}
