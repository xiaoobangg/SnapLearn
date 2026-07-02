package com.snaplearn.controller;

import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.RandomTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/random-test")
@RequiredArgsConstructor
public class RandomTestController {

    private final RandomTestService randomTestService;

    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody Map<String, Object> body, @CurrentUser String userId) {
        int count = body.containsKey("count") ? ((Number) body.get("count")).intValue() : 10;
        return randomTestService.start(userId, count);
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody Map<String, Object> body, @CurrentUser String userId) {
        @SuppressWarnings("unchecked")
        List<String> questionIds = (List<String>) body.getOrDefault("question_ids", List.of());
        @SuppressWarnings("unchecked")
        List<String> userAnswers = (List<String>) body.getOrDefault("user_answers", List.of());
        return randomTestService.submit(userId, questionIds, userAnswers);
    }

    /** Frontend calls this immediately when user selects a wrong answer */
    @PostMapping("/mark-wrong")
    public Map<String, Object> markWrong(@RequestBody Map<String, Object> body, @CurrentUser String userId) {
        String wordId = (String) body.get("word_id");
        String questionType = (String) body.get("question_type");
        randomTestService.markWrong(userId, wordId, questionType);
        return Map.of("ok", true);
    }
}
