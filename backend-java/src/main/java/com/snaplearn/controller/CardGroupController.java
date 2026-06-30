package com.snaplearn.controller;

import com.snaplearn.dto.request.CardCreateRequest;
import com.snaplearn.dto.request.MoveCardRequest;
import com.snaplearn.dto.response.CardGroupResponse;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.CardGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/card-groups")
@RequiredArgsConstructor
public class CardGroupController {

    private final CardGroupService cardGroupService;

    @PostMapping
    public CardGroupResponse create(@RequestBody @Valid CardCreateRequest req, @CurrentUser String userId) {
        return cardGroupService.create(userId, req);
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @CurrentUser String userId,
            @RequestParam(defaultValue = "false") boolean includeCompleted
    ) {
        return cardGroupService.listByUser(userId, includeCompleted);
    }

    @GetMapping("/{groupId}")
    public CardGroupResponse getById(@PathVariable String groupId, @CurrentUser String userId) {
        return cardGroupService.getById(groupId, userId);
    }

    @DeleteMapping("/{groupId}")
    public Map<String, Boolean> delete(@PathVariable String groupId, @CurrentUser String userId) {
        cardGroupService.delete(groupId, userId);
        return Map.of("ok", true);
    }

    @PostMapping("/cards/{cardId}/move")
    public Map<String, Object> moveCard(
            @PathVariable String cardId,
            @RequestBody MoveCardRequest req,
            @CurrentUser String userId
    ) {
        return cardGroupService.moveCard(cardId, userId, req.targetGroupId(), req.newGroupTitle());
    }

    // ===== v2.0 新增：学习流程 =====

    @PostMapping("/{groupId}/start-learning")
    public Map<String, String> startLearning(@PathVariable String groupId, @CurrentUser String userId) {
        cardGroupService.startLearning(groupId, userId);
        return Map.of("status", "learning");
    }

    @PostMapping("/cards/{cardId}/mark")
    public Map<String, Object> markCard(
            @PathVariable String cardId,
            @RequestParam boolean mastered,
            @CurrentUser String userId
    ) {
        return cardGroupService.markCard(cardId, userId, mastered);
    }

    @GetMapping("/{groupId}/learn-status")
    public Map<String, Object> learnStatus(@PathVariable String groupId, @CurrentUser String userId) {
        return cardGroupService.getLearnStatus(groupId, userId);
    }
}
