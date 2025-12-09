package com.newsapp.aggregator.service;

import com.newsapp.aggregator.model.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class ScraperService {

    // Helper class to define a "Pattern"
    private static class SelectorStrategy {
        String name;
        String cssQuery;

        public SelectorStrategy(String name, String cssQuery) {
            this.name = name;
            this.cssQuery = cssQuery;
        }
    }

    /**
     * MAIN METHOD 1: Finds Headlines (The "Menu")
     * Visits a Homepage (like zeenews.com) and extracts a list of articles.
     */
    public List<NewsItem> scrapeGeneric(String url, String sourceName) {
        List<NewsItem> newsList = new ArrayList<>();
        
        try {
            System.out.println("Connecting to: " + url + "...");
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(10000) // Increased timeout to 10 seconds
                    .get();

            List<SelectorStrategy> strategies = new ArrayList<>();

            // 1. MODERN & ZEE NEWS STYLE (Links that WRAP headings)
            strategies.add(new SelectorStrategy("Link Wrapping H1", "a:has(h1)"));
            strategies.add(new SelectorStrategy("Link Wrapping H2", "a:has(h2)"));
            strategies.add(new SelectorStrategy("Link Wrapping H3", "a:has(h3)"));

            // 2. CLASS-BASED TITLES (Common in WordPress/CMS)
            strategies.add(new SelectorStrategy("Article Title Class", ".article-title a"));
            strategies.add(new SelectorStrategy("Entry Title Class", ".entry-title a"));
            strategies.add(new SelectorStrategy("Post Title Class", ".post-title a"));
            strategies.add(new SelectorStrategy("News Headline Class", ".headline a"));
            strategies.add(new SelectorStrategy("Story Heading", ".story-heading a"));

            // 3. DIV-BASED TITLES (Common in Custom Portals)
            strategies.add(new SelectorStrategy("Generic Title Div", "div[class*='title'] a"));
            strategies.add(new SelectorStrategy("Generic Head Div", "div[class*='head'] a"));

            // 4. OLD SCHOOL (Headings that CONTAIN links)
            strategies.add(new SelectorStrategy("Standard H1", "h1 a"));
            strategies.add(new SelectorStrategy("Standard H2", "h2 a"));
            strategies.add(new SelectorStrategy("Standard H3", "h3 a"));
            
            // 5. LAST RESORT
            strategies.add(new SelectorStrategy("Bold Links", "a strong"));

            Elements foundElements = null;
            String usedStrategy = "None";

            // Loop through strategies to find the best match
            for (SelectorStrategy strategy : strategies) {
                Elements attempts = doc.select(strategy.cssQuery);
                if (attempts.size() > 3) {
                    foundElements = attempts;
                    usedStrategy = strategy.name;
                    System.out.println("✅ MATCH: " + sourceName + " uses strategy: [" + usedStrategy + "]");
                    break; 
                }
            }

            if (foundElements == null || foundElements.isEmpty()) {
                System.err.println("❌ FAILED: No recognizable news pattern found for " + url);
                return newsList;
            }

            // FILTER & SAVE
            for (Element element : foundElements) {
                String title = element.text().trim();
                String link = element.attr("abs:href"); // Handles relative URLs

                if (isValidNewsItem(title, link)) {
                    NewsItem item = new NewsItem();
                    item.setTitle(title);
                    item.setLink(link);
                    item.setPubDate(new Date());
                    item.setSource(sourceName);
                    
                    // Note: We leave description empty here. 
                    // The Scheduler will call scrapeContent() later to fill it.
                    
                    newsList.add(item);
                }
            }
            System.out.println("   -> Scraped " + newsList.size() + " valid items.");

        } catch (IOException e) {
            System.err.println("Error scraping " + url + ": " + e.getMessage());
        }
        return newsList;
    }

    /**
     * MAIN METHOD 2: Finds Content (The "Meal")
     * Visits a specific Article Page and extracts the body text.
     * This is the method your Scheduler was missing.
     */
    public String scrapeContent(String articleUrl) {
        try {
            // 1. Connect to the specific article
            Document doc = Jsoup.connect(articleUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

            // 2. Strategy: Try to find the main text area using common CSS classes
            Elements content = doc.select("div.article-body p, div.entry-content p, div.story-content p, div.post-content p, .article-content p");

            // 3. Fallback: If specific classes aren't found, check generic tags
            if (content.isEmpty()) {
                content = doc.select("article p"); 
            }
            if (content.isEmpty()) {
                // Last resort: Grab all paragraphs (might get sidebar text, but better than nothing)
                content = doc.select("p"); 
            }

            // 4. Return the text (Cleaned up)
            if (content.hasText()) {
                String fullText = content.text();
                // Optional: Truncate if you only want a preview
                 if (fullText.length() > 500) {
                     return fullText.substring(0, 500) + "...";
                 }
                return fullText;
            }

        } catch (IOException e) {
            System.err.println("Error scraping content for " + articleUrl + ": " + e.getMessage());
        }
        
        return "Content could not be fetched. Click the link to read.";
    }

    /**
     * Helper: Filter out garbage links
     */
    private boolean isValidNewsItem(String title, String link) {
        if (title.length() < 15) return false;
        if (link.isEmpty()) return false;
        
        String lowerTitle = title.toLowerCase();
        List<String> garbageWords = Arrays.asList(
            "privacy policy", "terms of use", "contact us", "subscribe", 
            "login", "sign in", "advertisement", "click here", "read more"
        );

        for (String garbage : garbageWords) {
            if (lowerTitle.contains(garbage)) return false;
        }
        return true;
    }

    // --- TEST METHOD (You can keep or remove this) ---
    public List<NewsItem> scrapeRainbowNews() {
        List<NewsItem> all = new ArrayList<>();
        all.addAll(scrapeGeneric("https://zeenews.india.com/", "Zee News"));
        all.addAll(scrapeGeneric("https://rainbownews.in/", "Rainbow News"));
        return all;
    }
}