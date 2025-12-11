📰 Smart News Aggregator System
A full-stack Java Spring Boot application that aggregates news from multiple sources (RSS Feeds & Web Scrapers), stores them in a relational database, and uses Google Gemini AI to generate automated daily news scripts.

🚀 Features
Hybrid Data Collection:

RSS Feeds: Fetches structured data from BBC News and Times of India.

Web Scraping: Uses Jsoup to scrape articles from non-RSS sites like Zee News and Rainbow News.

Smart Image Extraction: Advanced logic to find article thumbnails using OpenGraph tags, Twitter Cards, or content body images.

AI-Powered Summarization: Integrated Google Gemini 1.5 Flash API to read top headlines and generate a concise "News Anchor" script automatically.

Duplicate Detection: Intelligent scheduler checks the database to prevent saving the same article twice.

RESTful API: Exposes endpoints (/api/news) to serve JSON data to any frontend.

Modern Frontend: A responsive, grid-based news portal built with HTML5, CSS3, and JavaScript.

🛠️ Tech Stack
Backend: Java, Spring Boot (Web, Data JPA, Scheduler)

Database: MySQL (Hibernate ORM)

AI Engine: Google Gemini API (1.5 Flash)

Scraping: Jsoup Library

RSS Parsing: ROME Library

Frontend: Vanilla JavaScript, HTML5, CSS3

📂 Project Structure
Bash

src
├── main
│   ├── java
│   │   └── com.newsapp.aggregator
│   │       ├── controller   # REST Endpoints (NewsController)
│   │       ├── model        # Database Entities (NewsItem)
│   │       ├── repository   # JPA Repositories
│   │       ├── scheduler    # Cron Jobs (NewScheduler)
│   │       └── service      # Logic (Scraper, RSS, Gemini)
│   └── resources
│       ├── static           # Frontend (index.html)
│       └── application.properties # Config
⚙️ Setup & Installation
1. Prerequisites
Java JDK 21 or higher

MySQL Server installed and running

Maven installed (or use the wrapper included in Spring Boot)

A Google AI Studio API Key (Free)

2. Database Setup
Open your MySQL Workbench or Terminal and create the database:

SQL

CREATE DATABASE news_db;
3. Configure Application
Open src/main/resources/application.properties and update your MySQL credentials:

Properties

spring.datasource.url=jdbc:mysql://localhost:3306/news_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.jpa.hibernate.ddl-auto=update
4. Configure AI Key
Open src/main/java/com/newsapp/aggregator/service/NewsScriptService.java and paste your API key:

Java

private final String GEMINI_API_KEY = "YOUR_GOOGLE_GEMINI_KEY";
5. Run the Application
You can run it via Eclipse/IntelliJ or the command line:

Bash

mvn spring-boot:run
🖥️ Usage
Start the Server: The application will start on http://localhost:8080.

Background Tasks:

Every 60 seconds: The Scheduler fetches new articles from RSS and Scrapers.

Every Hour: The Scheduler triggers Gemini to write a new broadcast script.

View the Frontend: Open your browser and go to:

http://localhost:8080

Access the API:

http://localhost:8080/api/news (Returns JSON list of articles)

📸 Screenshots
(You can add the screenshots you shared with me here by uploading them to your repo's 'assets' folder)

News Grid Interface: Displays articles with images and sources.

JSON API Response: Shows the raw data structure.

🤝 Future Improvements
Integration with D-ID API to turn the text script into a generated video automatically.

User authentication to allow bookmarking articles.

Category filtering (Sports, Politics, Tech).

📝 License
This project is open-source and available for educational purposes.
