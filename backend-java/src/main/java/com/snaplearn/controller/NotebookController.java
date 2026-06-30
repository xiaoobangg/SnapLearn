package com.snaplearn.controller;

import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.NotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notebook")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @CurrentUser String userId
    ) {
        NotebookService.PageResult result = notebookService.listNotebook(userId, status, page, pageSize);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", result.items());
        resp.put("page", result.page());
        resp.put("page_size", result.pageSize());
        resp.put("total", result.total());
        return resp;
    }

    @DeleteMapping("/{notebookId}")
    public Map<String, Boolean> remove(@PathVariable String notebookId, @CurrentUser String userId) {
        notebookService.removeFromNotebook(notebookId, userId);
        return Map.of("ok", true);
    }
}
