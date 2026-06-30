package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snaplearn.entity.ApiAccessLog;
import com.snaplearn.mapper.ApiAccessLogMapper;
import com.snaplearn.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final ApiAccessLogMapper logMapper;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String uri
    ) {
        QueryWrapper<ApiAccessLog> qw = new QueryWrapper<>();
        if (uri != null && !uri.isEmpty()) {
            qw.like("uri", uri);
        }
        qw.orderByDesc("created_at");
        Page<ApiAccessLog> pg = logMapper.selectPage(new Page<>(page, pageSize), qw);
        List<Map<String, Object>> items = pg.getRecords().stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("user_id", log.getUserId());
            m.put("method", log.getMethod());
            m.put("uri", log.getUri());
            m.put("ip", log.getIp());
            m.put("request_body", log.getRequestBody() != null ? log.getRequestBody() : "");
            m.put("response_body", log.getResponseBody() != null ? log.getResponseBody() : "");
            m.put("duration_ms", log.getDurationMs());
            m.put("created_at", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
            return m;
        }).toList();
        return Map.of("items", items, "total", pg.getTotal(), "page", page, "page_size", pageSize);
    }
}
