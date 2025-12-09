package com.newsapp.aggregator.controller;

import com.newsapp.aggregator.model.NewsItem;
import com.newsapp.aggregator.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class NewsController {

    @Autowired
    private NewsRepository newsRepository;

    @GetMapping("/api/news")
    public List<NewsItem> getAggregatedNews() {
        // 1. Fetch from Database (Instant)
        // The Scheduler runs in the background to keep this data fresh.
        return newsRepository.findAllByOrderByPubDateDesc();
    }
}