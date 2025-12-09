package com.newsapp.aggregator.service;

import com.newsapp.aggregator.model.NewsItem;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class RssService {

    public List<NewsItem> parseRssFeed(String feedUrl) {
        List<NewsItem> newsList = new ArrayList<>();
        
        try {
            // 1. FIX: Use HttpURLConnection to set a User-Agent (Pretend to be Chrome)
            URL url = new URL(feedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0");
            connection.setConnectTimeout(5000); // Don't hang forever if site is down
            
            // 2. Feed Input using the stream from our safe connection
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(connection));

            for (SyndEntry entry : feed.getEntries()) {
                NewsItem item = new NewsItem();
                item.setTitle(entry.getTitle());
                item.setLink(entry.getLink());
                item.setPubDate(entry.getPublishedDate());
                
                // Safety check for description
                if (entry.getDescription() != null) {
                    // (Optional: You could use Jsoup here to clean HTML tags from description)
                    item.setDescription(entry.getDescription().getValue());
                }
                
                // Set the source based on the Feed Title (e.g., "BBC News")
                item.setSource(feed.getTitle() != null ? feed.getTitle() : "RSS Feed");
                
                newsList.add(item);
            }
        } catch (Exception e) {
            System.err.println("Error parsing RSS feed (" + feedUrl + "): " + e.getMessage());
        }
        
        return newsList;
    }
}