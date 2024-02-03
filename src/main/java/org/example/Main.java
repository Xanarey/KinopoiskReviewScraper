package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String BASE_URL = "https://www.kinopoisk.ru/film/";
    private static final int MAX_RETRIES = 3;

    public static void main(String[] args) {
        String filmId = "325";
        String url = BASE_URL + filmId + "/reviews/";
        String directoryName = "reviews/" + filmId;
        Elements reviews = null;

        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                Document doc = Jsoup.connect(url).get();
                reviews = doc.select(".reviewItem.userReview");
                break;
            } catch (IOException e) {
                logger.error("Ошибка подключения: " + e.getMessage());
                if (++attempts >= MAX_RETRIES) {
                    logger.error("Превышено максимальное количество попыток подключения.");
                    return;
                }
            }
        }

        String fileName = createFileName(directoryName, filmId);
        if (reviews != null) {
            writeToFile(reviews, fileName);
        }
    }

    private static String createFileName(String directoryName, String filmId) {
        new File(directoryName).mkdirs();
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return directoryName + "/reviews_" + filmId + "_" + timestamp + ".csv";
    }

    private static void writeToFile(Elements reviews, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Element review : reviews) {
                String csvLine = extractReviewData(review);
                writer.write(csvLine);
            }
        } catch (IOException e) {
            logger.error("Ошибка при записи в файл: " + e.getMessage());
        }
    }

    private static String extractReviewData(Element review) {
        StringBuilder sb = new StringBuilder();
        String dataId = review.attr("data-id");
        String author = review.select(".profile_name > a").first().text();
        String title = review.select(".sub_title").text();
        String reviewText = review.select(".brand_words").text();
        String date = review.select(".date").text();
        String rating = review.select("#comment_num_vote_" + dataId).text();

        sb.append(wrapInQuotes(author)).append(',')
                .append(wrapInQuotes(title)).append(',')
                .append(wrapInQuotes(reviewText)).append(',')
                .append(wrapInQuotes(date)).append(',')
                .append(wrapInQuotes(rating)).append('\n');
        return sb.toString();
    }

    private static String wrapInQuotes(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
