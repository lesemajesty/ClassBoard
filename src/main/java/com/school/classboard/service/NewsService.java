package com.school.classboard.service;

import com.school.classboard.domain.NewsItem;
import com.school.classboard.repository.NewsItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 精选新闻业务逻辑。
 */
@Service
@Transactional(readOnly = true)
public class NewsService {

    private final NewsItemRepository repository;

    public NewsService(NewsItemRepository repository) {
        this.repository = repository;
    }

    /** 班牌展示：启用中的新闻，推荐优先、时间倒序 */
    public List<NewsItem> latest(int limit) {
        return repository.findByActiveTrueOrderByFeaturedDescPublishedAtDesc()
                .stream().limit(limit).toList();
    }

    public List<NewsItem> findAll() {
        return repository.findAllByOrderByPublishedAtDesc();
    }

    public Optional<NewsItem> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    @Transactional
    public NewsItem save(NewsItem item) {
        return repository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
