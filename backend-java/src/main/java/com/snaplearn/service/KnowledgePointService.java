package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.CardKpProgressMapper;
import com.snaplearn.mapper.KnowledgePointMapper;
import com.snaplearn.mapper.WordContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;
    private final CardKpProgressMapper cardKpProgressMapper;
    private final WordContentMapper wordContentMapper;

    private static final List<String> POINT_ORDER = List.of(
            "pronunciation", "pos", "general_meaning", "extended_meaning",
            "example_sentence", "memory_tip"
    );

    /** Ensure KP index rows exist for a word. Content read from WordContent via JOIN at query time. */
    public List<KnowledgePoint> ensureForWord(String wordId) {
        QueryWrapper<KnowledgePoint> qw = new QueryWrapper<>();
        qw.eq("word_id", wordId);
        List<KnowledgePoint> existing = knowledgePointMapper.selectList(qw);
        if (!existing.isEmpty()) return existing;

        List<KnowledgePoint> points = new ArrayList<>();
        int order = 0;
        for (String type : POINT_ORDER) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setId(UUID.randomUUID().toString());
            kp.setWordId(wordId);
            kp.setPointType(type);
            kp.setSortOrder(order++);
            knowledgePointMapper.insert(kp);
            points.add(kp);
        }
        return points;
    }

    /** Get KPs with content from WordContent and status from card_kp_progress */
    public List<Map<String, Object>> getByCardIdWithProgress(String cardId, String wordId) {
        QueryWrapper<WordContent> wcq = new QueryWrapper<>();
        wcq.eq("word_id", wordId);
        WordContent wc = wordContentMapper.selectOne(wcq);
        QueryWrapper<KnowledgePoint> qw = new QueryWrapper<>();
        qw.eq("word_id", wordId).orderByAsc("sort_order");
        List<KnowledgePoint> kps = knowledgePointMapper.selectList(qw);

        QueryWrapper<CardKpProgress> pqw = new QueryWrapper<>();
        pqw.eq("card_id", cardId);
        Map<String, String> progressMap = new HashMap<>();
        cardKpProgressMapper.selectList(pqw).forEach(p -> progressMap.put(p.getKpId(), p.getStatus()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgePoint kp : kps) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", kp.getId());
            m.put("word_id", kp.getWordId());
            m.put("point_type", kp.getPointType());
            m.put("sort_order", kp.getSortOrder());
            m.put("content", getContentByType(wc, kp.getPointType()));
            m.put("status", progressMap.getOrDefault(kp.getId(), "unshown"));
            result.add(m);
        }
        return result;
    }

    public void initProgressForCard(String cardId, String wordId) {
        List<KnowledgePoint> kps = ensureForWord(wordId);
        for (KnowledgePoint kp : kps) {
            CardKpProgress p = new CardKpProgress();
            p.setId(UUID.randomUUID().toString());
            p.setCardId(cardId);
            p.setKpId(kp.getId());
            p.setStatus("unshown");
            cardKpProgressMapper.insert(p);
        }
    }

    public void markShown(String kpId, String cardId) {
        updateProgress(kpId, cardId, "shown");
    }

    public void markConfirmed(String kpId, String cardId) {
        updateProgress(kpId, cardId, "confirmed");
    }

    private void updateProgress(String kpId, String cardId, String status) {
        UpdateWrapper<CardKpProgress> uw = new UpdateWrapper<>();
        uw.eq("kp_id", kpId).eq("card_id", cardId).set("status", status);
        cardKpProgressMapper.update(null, uw);
    }

    public void resetForCard(String cardId) {
        UpdateWrapper<CardKpProgress> uw = new UpdateWrapper<>();
        uw.eq("card_id", cardId).set("status", "unshown");
        cardKpProgressMapper.update(null, uw);
    }

    public boolean allConfirmed(String cardId) {
        QueryWrapper<CardKpProgress> qw = new QueryWrapper<>();
        qw.eq("card_id", cardId);
        long total = cardKpProgressMapper.selectCount(qw);
        qw.eq("status", "confirmed");
        long confirmed = cardKpProgressMapper.selectCount(qw);
        return total > 0 && total == confirmed;
    }

    private String getContentByType(WordContent wc, String type) {
        if (wc == null) return "";
        return switch (type) {
            case "pronunciation" -> wc.getPronunciation();
            case "pos" -> wc.getPos();
            case "general_meaning" -> wc.getGeneralMeaning();
            case "extended_meaning" -> wc.getExtendedMeaning();
            case "example_sentence" -> wc.getExampleSentence();
            case "memory_tip" -> wc.getMemoryTip();
            default -> "";
        };
    }
}
