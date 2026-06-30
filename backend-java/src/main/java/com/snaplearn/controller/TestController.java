package com.snaplearn.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.ErrorBookService;
import com.snaplearn.service.TestService;
import com.snaplearn.service.TestService.AnswerItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final ErrorBookService errorBookService;

    @PostMapping("/groups/{groupId}/start")
    public Map<String, Object> startTest(@PathVariable String groupId, @CurrentUser String userId) {
        return testService.startTest(groupId, userId);
    }

    @PostMapping("/submit")
    public Map<String, Object> submitAnswers(@RequestBody SubmitRequest req, @CurrentUser String userId) {
        log.info("[TEST-SUBMIT] groupId={} answerCount={}", req.groupId(), req.answers() != null ? req.answers().size() : 0);
        return testService.submitAnswers(req.groupId(), userId, req.answers());
    }

    @GetMapping("/groups/{groupId}/result")
    public Map<String, Object> getResult(@PathVariable String groupId, @CurrentUser String userId) {
        return testService.getResult(groupId, userId);
    }

    @PostMapping("/groups/{groupId}/retry")
    public Map<String, Object> retryTest(@PathVariable String groupId, @CurrentUser String userId) {
        var questions = testService.getExistingQuestions(groupId, userId);
        return Map.of("questions", questions, "total", questions.size(), "group_id", groupId);
    }

    @GetMapping("/groups/{groupId}/errors")
    public Map<String, Object> getErrors(@PathVariable String groupId, @CurrentUser String userId) {
        var errors = errorBookService.getUnresolvedByGroup(groupId, userId);
        return Map.of("errors", errors, "total", errors.size());
    }

    @GetMapping("/groups/{groupId}/status")
    public Map<String, Object> getStatus(@PathVariable String groupId, @CurrentUser String userId) {
        return testService.getTestStatus(groupId, userId);
    }

    /** 调试：直接传 prompt + model，从日志复制 prompt 即可复测 */
    @PostMapping("/debug/generate-question")
    public Map<String, Object> debugGenerate(@RequestBody Map<String, Object> body) {
        String prompt = body.get("prompt") != null ? body.get("prompt").toString() : "";
        String model = body.get("model") != null ? body.get("model").toString() : "deepseek";
        return testService.debugGenerate(prompt, model);
    }

    public record SubmitRequest(@JsonProperty("groupId") String groupId, @JsonProperty("answers") List<AnswerItem> answers) {
    }
}
