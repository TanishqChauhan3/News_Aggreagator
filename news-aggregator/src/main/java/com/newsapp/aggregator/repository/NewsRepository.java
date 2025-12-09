package com.newsapp.aggregator.repository;

import com.newsapp.aggregator.model.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<NewsItem, Long> {
    
    // Check for duplicates
    boolean existsByLink(String link);

    // NEW: Fetch all news, sorted by newest date first
    List<NewsItem> findAllByOrderByPubDateDesc();
}