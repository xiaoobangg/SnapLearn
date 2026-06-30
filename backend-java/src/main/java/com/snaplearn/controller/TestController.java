package com.snaplearn.controller;

import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.ErrorBookService;
import com.snaplearn.service.TestService;
import com.snaplearn.service.TestService.AnswerItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final ErrorBookService errorBookService;

    @PostMapping("/groups/{groupId}/start")
    public Map<String, Object> startTest(@PathVariable String groupId, @CurrentUser String userId) {
        var questions = testService.generateTest(groupId, userId);
        return Map.of(
                "questions", questions,
                "total", questions.size(),
                "group_id", groupId
        );
    }

    @PostMapping("/submit")
    public Map<String, Object> submitAnswers(
            @RequestBody SubmitRequest req,
            @CurrentUser String userId
    ) {
        return testService.submitAnswers(req.groupId(), userId, req.answers());
    }

    @GetMapping("/groups/{groupId}/result")
    public Map<String, Object> getResult(@PathVariable String groupId, @CurrentUser String userId) {
        return testService.getResult(groupId, userId);
    }

    @PostMapping("/groups/{groupId}/retry")
    public Map<String, Object> retryTest(@PathVariable String groupId, @CurrentUser String userId) {
        var questions = testService.getExistingQuestions(groupId, userId);
        return Map.of(
                "questions", questions,
                "total", questions.size(),
                "group_id", groupId
        );
    }

    @GetMapping("/groups/{groupId}/errors")
    public Map<String, Object> getErrors(@PathVariable String groupId, @CurrentUser String userId) {
        var errors = errorBookService.getUnresolvedByGroup(groupId, userId);
        return Map.of("errors", errors, "total", errors.size());
    }

    public record SubmitRequest(String groupId, List<AnswerItem> answers) {
    }
}
