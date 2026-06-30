package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.dto.request.ReviewSubmitRequest;
import com.snaplearn.dto.response.ReviewCardResponse;
import com.snaplearn.entity.Notebook;
import com.snaplearn.mapper.NotebookMapper;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.ReviewService;
import com.snaplearn.service.ReviewService.ReviewResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {

    private final NotebookMapper notebookMapper;
    private final ReviewService reviewService;

    @GetMapping("/today")
    public Map<String, Object> getToday(@CurrentUser String userId) {
        List<ReviewCardResponse> cards = notebookMapper.selectTodayReviewCards(userId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("cards", cards);
        resp.put("total", cards.size());
        return resp;
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody @Valid ReviewSubmitRequest req, @CurrentUser String userId) {
        QueryWrapper<Notebook> qw = new QueryWrapper<>();
        qw.eq("card_id", req.cardId()).eq("user_id", userId);
        Notebook nb = notebookMapper.selectOne(qw);
        if (nb == null) {
            throw new BusinessException(404, "学习记录不存在");
        }

        ReviewResult result = reviewService.calculateNextReview(
                req.quality(),
                nb.getEaseFactor() != null ? nb.getEaseFactor() : 2.5,
                nb.getIntervalDays() != null ? nb.getIntervalDays() : 0,
                nb.getRepetitions() != null ? nb.getRepetitions() : 0
        );

        nb.setEaseFactor(result.easeFactor());
        nb.setIntervalDays(result.intervalDays());
        nb.setRepetitions(result.repetitions());
        nb.setStatus(result.status());
        nb.setNextReviewAt(java.time.LocalDateTime.parse(result.nextReviewAt(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        nb.setLastReviewAt(java.time.LocalDateTime.parse(result.lastReviewAt(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        notebookMapper.updateById(nb);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("ease_factor", result.easeFactor());
        resp.put("interval_days", result.intervalDays());
        resp.put("repetitions", result.repetitions());
        resp.put("status", result.status());
        resp.put("next_review_at", result.nextReviewAt());
        resp.put("last_review_at", result.lastReviewAt());
        return resp;
    }
}
