package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.dto.request.CardCreateRequest;
import com.snaplearn.dto.response.CardGroupResponse;
import com.snaplearn.dto.response.CardResponse;
import com.snaplearn.dto.response.KnowledgePointResponse;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.CardGroupMapper;
import com.snaplearn.mapper.CardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardGroupService {

    private final CardGroupMapper cardGroupMapper;
    private final CardMapper cardMapper;
    private final WordService wordService;
    private final WordContentService wordContentService;
    private final KnowledgePointService knowledgePointService;
    private final CardAudioAsyncService cardAudioAsyncService;
    private final VoiceService voiceService;

    /**
     * Create a card group with AI-generated cards
     */
    public CardGroupResponse create(String userId, CardCreateRequest req) {
        String groupId = UUID.randomUUID().toString();
        String src = req.sourceText() != null && !req.sourceText().isBlank() ? req.sourceText() : null;
        String title = src != null ? (src.length() > 50 ? src.substring(0, 50) : src) : "学习卡片组";

        CardGroup group = new CardGroup();
        group.setId(groupId);
        group.setUserId(userId);
        group.setSourceImage(req.sourceImage());
        group.setSourceText(req.sourceText());
        group.setTitle(title);
        group.setGroupStatus("pending");
        cardGroupMapper.insert(group);

        // 1. Ensure words exist
        List<Word> words = wordService.batchFindOrCreate(req.selectedWords());

        // 2. Generate word_contents via LLM
        String context = req.sourceText() != null && !req.sourceText().isBlank()
                ? req.sourceText()
                : String.join(", ", req.selectedWords());
        List<CardResponse> cardResponses = new ArrayList<>();
        List<Map<String, String>> audioItems = new ArrayList<>();
        int order = 0;

        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            String cardId = UUID.randomUUID().toString();

            // Ensure word_content exists (LLM generation)
            WordContent wc = wordContentService.ensureContent(word.getId(), word.getWordText(), context);

            // Create card (just the association)
            Card card = new Card();
            card.setId(cardId);
            card.setGroupId(groupId);
            card.setWordId(word.getId());
            card.setUserId(userId);
            card.setSortOrder(order++);
            card.setCardStatus("unlearned");
            cardMapper.insert(card);

            // Ensure KP index rows exist for this word, then init progress for this card
            knowledgePointService.ensureForWord(word.getId());
            knowledgePointService.initProgressForCard(cardId, word.getId());

            cardResponses.add(new CardResponse(
                    cardId, word.getWordText(),
                    wc.getGeneralMeaning(), wc.getExtendedMeaning(),
                    wc.getExampleSentence(), wc.getMemoryTip(),
                    wc.getPos(), wc.getPronunciation(), ""
            ));

            // 收集所有知识点文本用于异步音频预生成（按 word_id 复用）
            String wid = word.getId();
            addAudioItem(audioItems, wid, "word", word.getWordText());
            addAudioItem(audioItems, wid, "general_meaning", wc.getGeneralMeaning());
            addAudioItem(audioItems, wid, "extended_meaning", wc.getExtendedMeaning());
            addAudioItem(audioItems, wid, "example", wc.getExampleSentence());
            addAudioItem(audioItems, wid, "memory_tip", wc.getMemoryTip());
            addAudioItem(audioItems, wid, "pronunciation", wc.getPronunciation());
            addAudioItem(audioItems, wid, "pos", wc.getPos());
        }

        // 异步预生成单词 + 例句语音（不阻塞卡片组创建）
        try {
            cardAudioAsyncService.preGenerate(audioItems, voiceService.getDefault());
        } catch (Exception e) {
            log.warn("[CARD] audio preGen trigger failed, will retry on demand", e);
        }

        return new CardGroupResponse(
                groupId, title, req.sourceImage(), req.sourceText(), "", cardResponses
        );
    }

    /**
     * Start learning a card group
     */
    public void startLearning(String groupId, String userId) {
        CardGroup group = getGroupOrThrow(groupId, userId);
        if ("pending".equals(group.getGroupStatus()) || "testing".equals(group.getGroupStatus())) {
            group.setGroupStatus("learning");
            cardGroupMapper.updateById(group);
        }
    }

    /**
     * Mark a card as mastered or needs relearning
     */
    public Map<String, Object> markCard(String cardId, String userId, boolean mastered) {
        Card card = cardMapper.selectById(cardId);
        if (card == null || !card.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片不存在");
        }

        String newStatus = mastered ? "mastered" : "relearn";
        card.setCardStatus(newStatus);
        cardMapper.updateById(card);

        // If needs relearn, reset knowledge points
        if (!mastered) {
            knowledgePointService.resetForCard(cardId);
        }

        // Check if group learning is done
        String groupStatus = checkGroupLearnDone(card.getGroupId());
        return Map.of("ok", true, "card_status", newStatus, "group_status", groupStatus);
    }

    /**
     * Check if all cards in a group are mastered, update group status
     */
    private String checkGroupLearnDone(String groupId) {
        QueryWrapper<Card> allQw = new QueryWrapper<>();
        allQw.eq("group_id", groupId);
        long total = cardMapper.selectCount(allQw);

        QueryWrapper<Card> masteredQw = new QueryWrapper<>();
        masteredQw.eq("group_id", groupId).eq("card_status", "mastered");
        long mastered = cardMapper.selectCount(masteredQw);

        QueryWrapper<Card> relearnQw = new QueryWrapper<>();
        relearnQw.eq("group_id", groupId).eq("card_status", "relearn");
        long relearn = cardMapper.selectCount(relearnQw);

        CardGroup group = cardGroupMapper.selectById(groupId);
        String newStatus;
        if (total > 0 && mastered == total) {
            newStatus = "learn_done";
        } else if (relearn == 0 && total > 0) {
            newStatus = "learning";
        } else {
            newStatus = "learning"; // still has relearn items
        }

        if (group != null && !newStatus.equals(group.getGroupStatus())) {
            group.setGroupStatus(newStatus);
            cardGroupMapper.updateById(group);
        }
        return newStatus;
    }

    /**
     * Get cards that need relearning
     */
    public List<Card> getRelearnCards(String groupId, String userId) {
        QueryWrapper<Card> qw = new QueryWrapper<>();
        qw.eq("group_id", groupId).eq("user_id", userId).eq("card_status", "relearn");
        return cardMapper.selectList(qw);
    }

    /**
     * Reset relearn cards status for next round
     */
    public void startRelearnRound(String groupId, String userId) {
        UpdateWrapper<Card> uw = new UpdateWrapper<>();
        uw.eq("group_id", groupId).eq("user_id", userId)
                .eq("card_status", "relearn")
                .set("card_status", "unlearned");
        cardMapper.update(null, uw);

        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group != null) {
            group.setGroupStatus("learning");
            cardGroupMapper.updateById(group);
        }
    }

    public List<Map<String, Object>> listByUser(String userId, boolean includeCompleted) {
        QueryWrapper<CardGroup> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByDesc("created_at");
        List<CardGroup> groups = cardGroupMapper.selectList(qw);
        List<Map<String, Object>> results = new ArrayList<>();
        for (CardGroup g : groups) {
            QueryWrapper<Card> cardQw = new QueryWrapper<>();
            cardQw.eq("group_id", g.getId());
            long cardCount = cardMapper.selectCount(cardQw);
            cardQw.eq("card_status", "mastered");
            long masteredCount = cardMapper.selectCount(cardQw);

            // Hide completed groups (test_done) unless includeCompleted
            if (!includeCompleted && "test_done".equals(g.getGroupStatus())) {
                continue;
            }

            results.add(Map.of(
                    "id", g.getId(),
                    "title", g.getTitle() != null ? g.getTitle() : "",
                    "source_image", g.getSourceImage() != null ? g.getSourceImage() : "",
                    "source_text", g.getSourceText() != null ? g.getSourceText() : "",
                    "card_count", cardCount,
                    "mastered_count", masteredCount,
                    "group_status", g.getGroupStatus() != null ? g.getGroupStatus() : "",
                    "created_at", g.getCreatedAt() != null ? g.getCreatedAt().toString() : ""
            ));
        }
        return results;
    }

    public CardGroupResponse getById(String groupId, String userId) {
        CardGroup group = getGroupOrThrow(groupId, userId);

        QueryWrapper<Card> qw = new QueryWrapper<>();
        qw.eq("group_id", groupId).eq("user_id", userId).orderByAsc("sort_order");
        List<Card> cards = cardMapper.selectList(qw);

        // Batch fetch word info
        Set<String> wordIds = new LinkedHashSet<>();
        for (Card c : cards) {
            wordIds.add(c.getWordId());
        }
        Map<String, Word> wordMap = wordService.getByIds(wordIds);
        Map<String, WordContent> contentMap = wordContentService.getByWordIds(wordIds);

        List<CardResponse> cardResps = new ArrayList<>();
        for (Card c : cards) {
            Word word = wordMap.get(c.getWordId());
            WordContent wc = contentMap.get(c.getWordId());

            // Fetch knowledge points with progress for this card
            List<Map<String, Object>> kpMaps = knowledgePointService.getByCardIdWithProgress(c.getId(), c.getWordId());
            List<KnowledgePointResponse> kpResps = new ArrayList<>();
            for (Map<String, Object> kp : kpMaps) {
                kpResps.add(new KnowledgePointResponse(
                        (String) kp.get("id"), (String) kp.get("point_type"),
                        (String) kp.get("content"),
                        (String) kp.get("status"),
                        (Integer) kp.get("sort_order")
                ));
            }

            cardResps.add(new CardResponse(
                    c.getId(),
                    word != null ? word.getWordText() : "",
                    wc != null ? wc.getGeneralMeaning() : "",
                    wc != null ? wc.getExtendedMeaning() : "",
                    wc != null ? wc.getExampleSentence() : "",
                    wc != null ? wc.getMemoryTip() : "",
                    wc != null ? wc.getPos() : "",
                    wc != null ? wc.getPronunciation() : "",
                    c.getCreatedAt() != null ? c.getCreatedAt().toString() : "",
                    c.getCardStatus(),
                    kpResps
            ));
        }

        return new CardGroupResponse(
                group.getId(), group.getTitle(),
                group.getSourceImage(),
                group.getSourceText() != null ? group.getSourceText() : "",
                group.getCreatedAt() != null ? group.getCreatedAt().toString() : "",
                group.getGroupStatus(),
                cardResps
        );
    }

    /**
     * Get learning progress for a group
     */
    public Map<String, Object> getLearnStatus(String groupId, String userId) {
        getGroupOrThrow(groupId, userId);

        QueryWrapper<Card> allQw = new QueryWrapper<>();
        allQw.eq("group_id", groupId);
        long total = cardMapper.selectCount(allQw);

        allQw.eq("card_status", "mastered");
        long mastered = cardMapper.selectCount(allQw);

        QueryWrapper<Card> relearnQw = new QueryWrapper<>();
        relearnQw.eq("group_id", groupId).eq("card_status", "relearn");
        long relearn = cardMapper.selectCount(relearnQw);

        CardGroup group = cardGroupMapper.selectById(groupId);
        return Map.of(
                "total_cards", total,
                "mastered", mastered,
                "relearn", relearn,
                "unlearned", total - mastered - relearn,
                "group_status", group != null ? group.getGroupStatus() : ""
        );
    }

    public void delete(String groupId, String userId) {
        QueryWrapper<CardGroup> qw = new QueryWrapper<>();
        qw.eq("id", groupId).eq("user_id", userId);
        cardGroupMapper.delete(qw);
    }

    public Map<String, Object> moveCard(String cardId, String userId, String targetGroupId, String newGroupTitle) {
        Card card = cardMapper.selectById(cardId);
        if (card == null || !card.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片不存在");
        }

        String destGroupId;
        if (targetGroupId != null) {
            CardGroup targetGroup = cardGroupMapper.selectById(targetGroupId);
            if (targetGroup == null || !targetGroup.getUserId().equals(userId)) {
                throw new BusinessException(404, "目标卡片组不存在");
            }
            destGroupId = targetGroupId;
        } else if (newGroupTitle != null) {
            String newId = UUID.randomUUID().toString();
            CardGroup newGroup = new CardGroup();
            newGroup.setId(newId);
            newGroup.setUserId(userId);
            newGroup.setTitle(newGroupTitle);
            newGroup.setGroupStatus("pending");
            cardGroupMapper.insert(newGroup);
            destGroupId = newId;
        } else {
            throw new BusinessException(400, "请指定目标卡片组或新建卡片组标题");
        }

        card.setGroupId(destGroupId);
        cardMapper.updateById(card);

        return Map.of("ok", true, "target_group_id", destGroupId);
    }

    private void addAudioItem(List<Map<String, String>> items, String wordId, String type, String text) {
        if (text != null && !text.isBlank()) {
            Map<String, String> item = new HashMap<>();
            item.put("wordId", wordId);
            item.put("type", type);
            item.put("text", text);
            items.add(item);
        }
    }

    private CardGroup getGroupOrThrow(String groupId, String userId) {
        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片组不存在");
        }
        return group;
    }
}
