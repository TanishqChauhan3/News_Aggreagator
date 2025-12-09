package com.newsapp.aggregator.Scheduler;

import com.newsapp.aggregator.model.NewsItem;
import com.newsapp.aggregator.repository.NewsRepository;
import com.newsapp.aggregator.service.RssService;
import com.newsapp.aggregator.service.ScraperService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NewScheduler {

    @Autowired
    private RssService rssService;

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private NewsRepository newsRepository;

    private final Map<String, String> rssSources = new HashMap<>();
    private final Map<String, String> scrapeSources = new HashMap<>();

    public NewScheduler() {
        // --- 1. RSS SOURCES ---
        rssSources.put("BBC News", "https://feeds.bbci.co.uk/news/world/rss.xml");
        rssSources.put("Times of India", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms");

        // --- 2. SCRAPER SOURCES ---
        scrapeSources.put("Zee News", "https://zeenews.india.com/");
        scrapeSources.put("Rainbow News", "https://rainbownews.in/");
    }

    // Runs Every 60 Seconds
    @Scheduled(fixedRate = 60000) 
    public void fetchLatestNews() {
        System.out.println("--- 🚀 Starting Hybrid Update Cycle ---");

        // PHASE 1: Process RSS Feeds
        for (Map.Entry<String, String> entry : rssSources.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            try {
                List<NewsItem> items = rssService.parseRssFeed(url);
                processNewsItems(items, name);
            } catch (Exception e) {
                System.err.println("Error fetching RSS " + name + ": " + e.getMessage());
            }
        }

        // PHASE 2: Process Scraper Sites
        for (Map.Entry<String, String> entry : scrapeSources.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            try {
                List<NewsItem> items = scraperService.scrapeGeneric(url, name);
                processNewsItems(items, name);
            } catch (Exception e) {
                System.err.println("Error scraping " + name + ": " + e.getMessage());
            }
        }
        
        System.out.println("--- ✅ Cycle Complete ---");
    }

    // --- HELPER METHOD TO SAVE DATA ---
    private void processNewsItems(List<NewsItem> items, String sourceName) {
        if (items.isEmpty()) return;

        int newCount = 0;
        for (NewsItem item : items) {
            
            // 1. DUPLICATE CHECK
            if (!newsRepository.existsByLink(item.getLink())) {
                
                // 2. ENRICHMENT: Get the full body text
                String fullContent = scraperService.scrapeContent(item.getLink());
                item.setDescription(fullContent);

                // 3. IMPROVED IMAGE EXTRACTION
                try {
                    // Connect with a real Browser User-Agent to avoid blocking
                    Document doc = Jsoup.connect(item.getLink())
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                            .timeout(10000)
                            .get();
                    
                    String foundImageUrl = null;

                    // Strategy A: OpenGraph Image (Facebook Standard - Best Quality)
                    Element ogImage = doc.selectFirst("meta[property='og:image']");
                    if (ogImage != null) foundImageUrl = ogImage.attr("content");

                    // Strategy B: Twitter Card Image (If A fails)
                    if (foundImageUrl == null || foundImageUrl.isEmpty()) {
                        Element twImage = doc.selectFirst("meta[name='twitter:image']");
                        if (twImage != null) foundImageUrl = twImage.attr("content");
                    }

                    // Strategy C: Link Image Src (Old Standard)
                    if (foundImageUrl == null || foundImageUrl.isEmpty()) {
                        Element linkImg = doc.selectFirst("link[rel='image_src']");
                        if (linkImg != null) foundImageUrl = linkImg.attr("href");
                    }

                    // Strategy D: Look for images inside the main article body (Avoids logos/ads)
                    if (foundImageUrl == null || foundImageUrl.isEmpty()) {
                        // These are common class names for main content areas
                        Element bodyImg = doc.selectFirst("div.article-body img, div.entry-content img, div.post-content img, article img, figure img");
                        if (bodyImg != null) {
                            foundImageUrl = bodyImg.attr("abs:src");
                        }
                    }

                    // Strategy E: Last Resort - Grab the first image on the page that isn't tiny
                    if (foundImageUrl == null || foundImageUrl.isEmpty()) {
                        Element anyImg = doc.selectFirst("img");
                        if (anyImg != null) foundImageUrl = anyImg.attr("abs:src");
                    }

                    // Save the found image
                    if (foundImageUrl != null && !foundImageUrl.isEmpty()) {
                        item.setImageUrl(foundImageUrl);
                    }

                } catch (Exception e) {
                    System.err.println("Could not fetch image for: " + item.getLink());
                }

                item.setSource(sourceName);

                // 4. SAVE
                newsRepository.save(item);
                newCount++;
                System.out.println("   [SAVED] " + item.getTitle());
            }
        }
        if (newCount > 0) System.out.println("   -> Added " + newCount + " articles from " + sourceName);
    }
}