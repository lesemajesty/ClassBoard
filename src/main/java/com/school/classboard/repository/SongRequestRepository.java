package com.school.classboard.repository;

import com.school.classboard.domain.SongRequest;
import com.school.classboard.domain.SongStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRequestRepository extends JpaRepository<SongRequest, Long> {

    List<SongRequest> findAllByOrderByCreatedAtDesc();

    List<SongRequest> findByStatusOrderByCreatedAtDesc(SongStatus status);

    List<SongRequest> findByStatusInOrderByCreatedAtDesc(Collection<SongStatus> statuses);

    List<SongRequest> findByClassIdOrderByCreatedAtDesc(Long classId);
}
