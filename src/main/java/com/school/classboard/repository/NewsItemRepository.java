package com.school.classboard.repository;

import com.school.classboard.domain.NewsItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    List<NewsItem> findByActiveTrueOrderByFeaturedDescPublishedAtDesc();

    List<NewsItem> findAllByOrderByPublishedAtDesc();

    Optional<NewsItem> findByExternalKey(String externalKey);

    void deleteByExternalKeyIsNotNullAndPublishedAtBefore(LocalDateTime time);
}
