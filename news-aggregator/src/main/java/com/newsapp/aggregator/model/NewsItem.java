package com.newsapp.aggregator.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "news_articles")
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String description;

    // --- 🔴 ADD THIS SECTION START 🔴 ---
    @Column(length = 1000)
    private String imageUrl;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    // --- 🔴 ADD THIS SECTION END 🔴 ---

    private Date pubDate;
    private String source;

    // ... (Keep your other getters and setters for title, link, date, etc.) ...
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getPubDate() { return pubDate; }
    public void setPubDate(Date pubDate) { this.pubDate = pubDate; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}