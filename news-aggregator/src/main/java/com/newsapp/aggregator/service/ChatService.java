package com.newsapp.aggregator.service;

import com.newsapp.aggregator.repository.NewsRepository;
import com.newsapp.aggregator.model.NewsItem;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.JSONObject;
import org.json.JSONArray;

@Service
public class ChatService {

    private final String API_KEY = "";
    private final NewsRepository newsRepository;

    private NewsItem lastSelectedNews = null;

    public ChatService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public String getResponse(String userMessage) {
        try {

            String msg = userMessage.toLowerCase();
            List<NewsItem> news = newsRepository.findAll();

            if (news.isEmpty()) {
                return "📰 News is loading...";
            }

            //  TOP / LATEST NEWS
            if (msg.contains("top") || msg.contains("headline") || msg.contains("latest")) {
                return buildTop(news);
            }

            // CATEGORY
            if (msg.contains("tech") || msg.contains("sports") ||
                msg.contains("india") || msg.contains("international") ||
                msg.contains("weather")) {

                return categoryNews(news, msg);
            }

            // INDEX BASED
            int index = extractIndex(msg);
            if (index != -1 && index < news.size()) {

                NewsItem n = news.get(index);
                lastSelectedNews = n;

                if (msg.contains("summarize")) return summarize(n);
                if (msg.contains("detail") || msg.contains("explain")) return detail(n);

                return n.getTitle();
            }

            // FOLLOW-UP MEMORY
            if ((msg.contains("more") || msg.contains("detail")) && lastSelectedNews != null) {
                return detail(lastSelectedNews);
            }

            // KEYWORD MATCH
            NewsItem relevant = findRelevant(news, msg);
            lastSelectedNews = relevant;

            if (msg.contains("summarize")) return summarize(relevant);
            if (msg.contains("detail") || msg.contains("explain")) return detail(relevant);
            if (msg.contains("link")) return "👉 " + formatLink(relevant.getLink());

            if (msg.contains("news")) return relevant.getTitle();

            // NORMAL CHAT
            return callAI(userMessage);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // CATEGORY FILTER (FINAL FIX)
    private String categoryNews(List<NewsItem> news, String msg) {

        List<NewsItem> filtered = new ArrayList<>();

        List<String> techKeywords = Arrays.asList(
                "ai", "artificial intelligence", "software",
                "technology", "startup", "app",
                "cybersecurity", "hacker",
                "google", "microsoft", "apple",
                "meta", "openai", "chatgpt"
        );

        List<String> sportsKeywords = Arrays.asList(
                "football", "cricket", "match", "player"
        );

        List<String> weatherKeywords = Arrays.asList(
                "rain", "storm", "weather", "climate", "temperature"
        );

        List<String> internationalKeywords = Arrays.asList(
                "iran", "israel", "ukraine", "war", "china", "us"
        );

        List<String> indiaKeywords = Arrays.asList(
                "india", "delhi", "mumbai"
        );

        for (NewsItem n : news) {

            String t = n.getTitle().toLowerCase();
            String d = n.getDescription().toLowerCase();

            if (msg.contains("tech")) {
                if (containsAny(t, techKeywords) || containsAny(d, techKeywords)) {
                    filtered.add(n);
                }
            }

            if (msg.contains("sports")) {
                if (containsAny(t, sportsKeywords)) {
                    filtered.add(n);
                }
            }

            if (msg.contains("weather")) {
                if (containsAny(t, weatherKeywords)) {
                    filtered.add(n);
                }
            }

            if (msg.contains("international")) {
                if (containsAny(t, internationalKeywords)) {
                    filtered.add(n);
                }
            }

            if (msg.contains("india")) {
                if (containsAny(t, indiaKeywords) || containsAny(d, indiaKeywords)) {
                    filtered.add(n);
                }
            }
        }

        // 🔥 REMOVE DUPLICATES
        filtered = filtered.stream().distinct().toList();

        //  NO WRONG FALLBACK
        if (filtered.isEmpty()) {
            return "⚠️ No " + msg + " found in current news.\n\n👉 Try 'top news'.";
        }

        StringBuilder sb = new StringBuilder("📰 " + msg.toUpperCase() + ":\n\n");

        for (int i = 0; i < Math.min(5, filtered.size()); i++) {
            sb.append("• ").append(filtered.get(i).getTitle()).append("\n");
        }

        return sb.toString();
    }

    // HELPER
    private boolean containsAny(String text, List<String> keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    // TOP NEWS
    private String buildTop(List<NewsItem> news) {
        StringBuilder sb = new StringBuilder("📰 Top Headlines:\n\n");

        for (int i = 0; i < Math.min(5, news.size()); i++) {
            sb.append(i + 1).append(". ").append(news.get(i).getTitle()).append("\n");
        }

        return sb.toString();
    }

    // NDEX
    private int extractIndex(String msg) {
        if (msg.contains("1st")) return 0;
        if (msg.contains("2nd")) return 1;
        if (msg.contains("3rd")) return 2;
        if (msg.contains("4th")) return 3;
        if (msg.contains("5th")) return 4;
        return -1;
    }

    // KEYWORD MATCH
    private NewsItem findRelevant(List<NewsItem> news, String msg) {

        msg = msg.replace("news", "")
                 .replace("about", "")
                 .replace("summarize", "")
                 .trim();

        String[] words = msg.split(" ");

        for (NewsItem n : news) {
            String t = n.getTitle().toLowerCase();
            String d = n.getDescription().toLowerCase();

            for (String w : words) {
                if (w.length() > 2 && (t.contains(w) || d.contains(w))) {
                    return n;
                }
            }
        }

        return news.get(0);
    }

    //  SUMMARY
    private String summarize(NewsItem n) throws Exception {
        return callAI("Summarize in 2 lines:\n" + n.getTitle() + " - " + n.getDescription());
    }

    //  DETAIL
    private String detail(NewsItem n) throws Exception {
        String ai = callAI("Explain clearly:\n" + n.getTitle() + " - " + n.getDescription());
        return ai + "\n\n👉 " + formatLink(n.getLink());
    }

    private String formatLink(String url) {
        return "<a href=\"" + url + "\" target=\"_blank\">Read Article</a>";
    }

    //  AI CALL
    private String callAI(String prompt) throws Exception {

        JSONObject body = new JSONObject();
        body.put("model", "openai/gpt-3.5-turbo");

        JSONArray messages = new JSONArray();

        messages.put(new JSONObject().put("role", "system").put("content", "You are helpful."));
        messages.put(new JSONObject().put("role", "user").put("content", prompt));

        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://openrouter.ai/api/v1/chat/completions"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(response.body());

        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
}