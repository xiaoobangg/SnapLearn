package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.Card;
import com.snaplearn.entity.UserSettings;
import com.snaplearn.entity.WordBank;
import com.snaplearn.mapper.CardMapper;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final DailyCheckinService dailyCheckinService;
    private final WordBankService wordBankService;
    private final UserSettingsService userSettingsService;
    private final CardGroupService cardGroupService;
    private final CardMapper cardMapper;
    private final WordService wordService;

    // ===== 每日打卡 =====

    @GetMapping("/today")
    public Map<String, Object> getToday(
            @CurrentUser String userId,
            @RequestParam String bankId
    ) {
        return dailyCheckinService.getTodayWords(userId, bankId);
    }

    @PostMapping("/mark")
    public Map<String, Object> markWord(
            @RequestBody MarkRequest req,
            @CurrentUser String userId
    ) {
        return dailyCheckinService.markWord(req.poolId(), userId, req.mark());
    }

    @PostMapping("/complete")
    public Map<String, String> completeCheckin(
            @RequestBody CompleteRequest req,
            @CurrentUser String userId
    ) {
        dailyCheckinService.logCheckin(
                userId, req.bankId(), req.newCount(), req.reviewCount(),
                req.knownCount(), req.fuzzyCount(), req.unknownCount()
        );
        return Map.of("ok", "true");
    }

    @GetMapping("/calendar")
    public Map<String, Object> getCalendar(
            @CurrentUser String userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return dailyCheckinService.getCalendar(userId, year, month);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(@CurrentUser String userId) {
        return dailyCheckinService.getStats(userId);
    }

    // ===== 词库管理 =====

    @GetMapping("/banks")
    public Map<String, Object> listBanks(@CurrentUser String userId) {
        List<WordBank> banks = wordBankService.listByUser(userId);
        return Map.of("banks", banks);
    }

    @PostMapping("/banks")
    public Map<String, Object> createBank(
            @RequestBody CreateBankRequest req,
            @CurrentUser String userId
    ) {
        WordBank bank = wordBankService.create(req.name(), "user", userId, req.description());
        return Map.of("bank", bank);
    }

    @GetMapping("/banks/{bankId}")
    public Map<String, Object> bankDetail(@PathVariable String bankId, @CurrentUser String userId) {
        WordBank bank = wordBankService.getById(bankId);
        if (bank == null) throw new BusinessException(404, "词库不存在");
        var words = wordBankService.getBankWords(bankId);
        return Map.of("bank", bank, "words", words);
    }

    @DeleteMapping("/banks/{bankId}")
    public Map<String, String> deleteBank(@PathVariable String bankId, @CurrentUser String userId) {
        wordBankService.delete(bankId, userId);
        return Map.of("ok", "true");
    }

    @PostMapping("/banks/{bankId}/add-words")
    public Map<String, String> addWordsToBank(
            @PathVariable String bankId,
            @RequestBody AddWordsRequest req,
            @CurrentUser String userId
    ) {
        for (String wordText : req.words()) {
            wordBankService.addWord(bankId, wordText, userId);
        }
        return Map.of("ok", "true");
    }

    /**
     * Import words from a card group into a bank
     */
    @PostMapping("/banks/import-from-group")
    public Map<String, String> importFromGroup(
            @RequestBody ImportRequest req,
            @CurrentUser String userId
    ) {
        List<Card> cards = cardMapper.selectList(
                new QueryWrapper<Card>().eq("group_id", req.groupId()).eq("user_id", userId)
        );
        if (cards.isEmpty()) throw new BusinessException(400, "卡片组中没有卡片");
        wordBankService.importFromCardGroup(req.bankId(), cards, userId);
        return Map.of("ok", "true", "count", String.valueOf(cards.size()));
    }

    /**
     * Create a card group from selected checkin words
     */
    @PostMapping("/words-to-group")
    public Map<String, Object> wordsToGroup(
            @RequestBody WordsToGroupRequest req,
            @CurrentUser String userId
    ) {
        var groupResp = cardGroupService.create(userId, new com.snaplearn.dto.request.CardCreateRequest(
                null, "从每日打卡创建", req.wordTexts()
        ));
        return Map.of("group", groupResp);
    }

    // ===== 用户设置 =====

    @GetMapping("/settings")
    public Map<String, Object> getSettings(@CurrentUser String userId) {
        UserSettings settings = userSettingsService.getOrCreate(userId);
        return Map.of("settings", settings);
    }

    @PutMapping("/settings")
    public Map<String, Object> updateSettings(
            @RequestBody UpdateSettingsRequest req,
            @CurrentUser String userId
    ) {
        UserSettings settings = userSettingsService.update(
                userId, req.dailyNewWords(), req.dailyReviewWords(),
                req.checkinReminder(), req.reminderTime()
        );
        return Map.of("settings", settings);
    }

    // ===== AI 对话偏好 =====

    @GetMapping("/settings/chat")
    public Map<String, Object> getChatPrefs(@CurrentUser String userId) {
        return userSettingsService.getChatPreferences(userId);
    }

    @PutMapping("/settings/chat")
    public Map<String, Object> updateChatPrefs(@RequestBody Map<String, Object> body,
                                                @CurrentUser String userId) {
        String chatMode = (String) body.get("chat_mode");
        String chatModel = (String) body.get("chat_model");
        Boolean chatStream = body.get("chat_stream") instanceof Boolean b ? b : null;
        userSettingsService.updateChatPreferences(userId, chatMode, chatModel, chatStream);
        return Map.of("ok", true);
    }

    // ===== Request records =====

    public record MarkRequest(@com.fasterxml.jackson.annotation.JsonProperty("poolId") String poolId,
                              @com.fasterxml.jackson.annotation.JsonProperty("mark") String mark) {
    }

    public record CompleteRequest(@com.fasterxml.jackson.annotation.JsonProperty("bankId") String bankId,
                                  @com.fasterxml.jackson.annotation.JsonProperty("newCount") int newCount,
                                  @com.fasterxml.jackson.annotation.JsonProperty("reviewCount") int reviewCount,
                                  @com.fasterxml.jackson.annotation.JsonProperty("knownCount") int knownCount,
                                  @com.fasterxml.jackson.annotation.JsonProperty("fuzzyCount") int fuzzyCount,
                                  @com.fasterxml.jackson.annotation.JsonProperty("unknownCount") int unknownCount) {
    }

    public record CreateBankRequest(String name, String description) {
    }

    public record AddWordsRequest(List<String> words) {
    }

    public record ImportRequest(String bankId, String groupId) {
    }

    public record WordsToGroupRequest(List<String> wordTexts) {
    }

    public record UpdateSettingsRequest(Integer dailyNewWords, Integer dailyReviewWords,
                                        Boolean checkinReminder, java.time.LocalTime reminderTime) {
    }
}
