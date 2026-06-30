package com.snaplearn.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Deprecated // v2.0 — replaced by DailyCheckinService (百词斩 algorithm)
@Service
public class ReviewService {

    public ReviewResult calculateNextReview(int quality, double easeFactor, int intervalDays, int repetitions) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("quality must be 0-5");
        }

        int newInterval;
        int newReps;
        double newEase;
        String newStatus;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (quality < 3) {
            newInterval = 1;
            newReps = 0;
            newEase = Math.max(1.3, easeFactor - 0.2);
            newStatus = "learning";
        } else {
            if (repetitions == 0) {
                newInterval = 1;
            } else if (repetitions == 1) {
                newInterval = 3;
            } else {
                newInterval = (int) Math.round(intervalDays * easeFactor);
            }

            newReps = repetitions + 1;
            newEase = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
            newEase = Math.max(1.3, newEase);

            if (newReps >= 5) {
                newStatus = "mastered";
            } else if (newReps >= 2) {
                newStatus = "review";
            } else {
                newStatus = "learning";
            }
        }

        LocalDateTime nextReview = now.plusDays(newInterval);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        return new ReviewResult(
                Math.round(newEase * 100.0) / 100.0,
                newInterval,
                newReps,
                newStatus,
                nextReview.format(fmt),
                now.format(fmt)
        );
    }

    public record ReviewResult(
            double easeFactor,
            int intervalDays,
            int repetitions,
            String status,
            String nextReviewAt,
            String lastReviewAt
    ) {
    }
}
