package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.Feedback;
import com.snaplearn.entity.FeedbackReply;
import com.snaplearn.mapper.FeedbackMapper;
import com.snaplearn.mapper.FeedbackReplyMapper;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackMapper feedbackMapper;
    private final FeedbackReplyMapper replyMapper;
    private final JwtUtil jwtUtil;

    private String tryGetUserId(HttpServletRequest req) {
        try {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                return jwtUtil.validateAndGetClaims(header.substring(7)).getSubject();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** 用户提交反馈（公开，登录用户自动关联 userId） */
    @PostMapping("/feedback")
    public Map<String, Object> submit(@RequestBody Map<String, String> body,
                                       HttpServletRequest req) {
        String userId = tryGetUserId(req);
        Feedback fb = new Feedback();
        fb.setId(UUID.randomUUID().toString());
        fb.setUserId(userId);
        fb.setContent(body.getOrDefault("content", ""));
        fb.setStatus("pending");
        fb.setCreatedAt(LocalDateTime.now());
        fb.setUpdatedAt(LocalDateTime.now());
        feedbackMapper.insert(fb);
        return Map.of("ok", true, "id", fb.getId());
    }

    /** 用户查看自己的反馈及回复 */
    @GetMapping("/feedback/my")
    public List<Map<String, Object>> myFeedback(@CurrentUser String userId) {
        QueryWrapper<Feedback> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByDesc("created_at");
        return feedbackMapper.selectList(qw).stream().map(this::toVo).toList();
    }

    // ==================== 管理端 ====================

    /** 管理端列表 */
    @GetMapping("/admin/feedbacks")
    public List<Map<String, Object>> list() {
        QueryWrapper<Feedback> qw = new QueryWrapper<>();
        qw.orderByDesc("created_at");
        return feedbackMapper.selectList(qw).stream().map(this::toVo).toList();
    }

    /** 管理端回复 */
    @PostMapping("/admin/feedbacks/{id}/reply")
    public Map<String, Object> reply(@PathVariable String id, @RequestBody Map<String, String> body) {
        FeedbackReply reply = new FeedbackReply();
        reply.setId(UUID.randomUUID().toString());
        reply.setFeedbackId(id);
        reply.setContent(body.getOrDefault("content", ""));
        reply.setCreatedAt(LocalDateTime.now());
        replyMapper.insert(reply);

        Feedback fb = feedbackMapper.selectById(id);
        if (fb != null) {
            fb.setStatus("replied");
            fb.setUpdatedAt(LocalDateTime.now());
            feedbackMapper.updateById(fb);
        }
        return Map.of("ok", true);
    }

    private Map<String, Object> toVo(Feedback fb) {
        QueryWrapper<FeedbackReply> qw = new QueryWrapper<>();
        qw.eq("feedback_id", fb.getId()).orderByAsc("created_at");
        List<Map<String, Object>> replies = replyMapper.selectList(qw).stream()
                .map(r -> Map.<String, Object>of("content", r.getContent(), "created_at", r.getCreatedAt().toString()))
                .toList();

        Map<String, Object> m = new HashMap<>();
        m.put("id", fb.getId());
        m.put("content", fb.getContent());
        m.put("status", fb.getStatus());
        m.put("created_at", fb.getCreatedAt() != null ? fb.getCreatedAt().toString() : "");
        m.put("replies", replies);
        return m;
    }
}
