package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.snaplearn.entity.KnowledgePoint;
import com.snaplearn.entity.WordContent;
import com.snaplearn.mapper.KnowledgePointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "pronunciation", "音标",
            "pos", "词性",
            "general_meaning", "释义",
            "extended_meaning", "语境含义",
            "example_sentence", "例句",
            "memory_tip", "记忆技巧"
    );

    private static final List<String> POINT_ORDER = List.of(
            "pronunciation", "pos", "general_meaning", "extended_meaning",
            "example_sentence", "memory_tip"
    );

    /**
     * Generate knowledge points from word_content for a card
     */
    public List<KnowledgePoint> createForCard(String wordId, String cardId, WordContent wc) {
        List<KnowledgePoint> points = new ArrayList<>();
        int order = 0;
        for (String type : POINT_ORDER) {
            String content = getContentByType(wc, type);
            if (content == null || content.isBlank()) continue;

            KnowledgePoint kp = new KnowledgePoint();
            kp.setId(UUID.randomUUID().toString());
            kp.setWordId(wordId);
            kp.setCardId(cardId);
            kp.setPointType(type);
            kp.setContent(content);
            kp.setSortOrder(order++);
            kp.setStatus("unshown");
            knowledgePointMapper.insert(kp);
            points.add(kp);
        }
        return points;
    }

    /**
     * Get knowledge points for a card, ordered by sort_order
     */
    public List<KnowledgePoint> getByCardId(String cardId) {
        QueryWrapper<KnowledgePoint> qw = new QueryWrapper<>();
        qw.eq("card_id", cardId).orderByAsc("sort_order");
        return knowledgePointMapper.selectList(qw);
    }

    /**
     * Mark a knowledge point as shown
     */
    public void markShown(String pointId) {
        UpdateWrapper<KnowledgePoint> uw = new UpdateWrapper<>();
        uw.eq("id", pointId).set("status", "shown");
        knowledgePointMapper.update(null, uw);
    }

    /**
     * Mark a knowledge point as confirmed
     */
    public void markConfirmed(String pointId) {
        UpdateWrapper<KnowledgePoint> uw = new UpdateWrapper<>();
        uw.eq("id", pointId).set("status", "confirmed");
        knowledgePointMapper.update(null, uw);
    }

    /**
     * Reset all points for a card to unshown (for relearning)
     */
    public void resetForCard(String cardId) {
        UpdateWrapper<KnowledgePoint> uw = new UpdateWrapper<>();
        uw.eq("card_id", cardId).set("status", "unshown");
        knowledgePointMapper.update(null, uw);
    }

    /**
     * Check if all points for a card are confirmed
     */
    public boolean allConfirmed(String cardId) {
        QueryWrapper<KnowledgePoint> qw = new QueryWrapper<>();
        qw.eq("card_id", cardId);
        long total = knowledgePointMapper.selectCount(qw);
        qw.eq("status", "confirmed");
        long confirmed = knowledgePointMapper.selectCount(qw);
        return total > 0 && total == confirmed;
    }

    private String getContentByType(WordContent wc, String type) {
        return switch (type) {
            case "pronunciation" -> wc.getPronunciation();
            case "pos" -> wc.getPos();
            case "general_meaning" -> wc.getGeneralMeaning();
            case "extended_meaning" -> wc.getExtendedMeaning();
            case "example_sentence" -> wc.getExampleSentence();
            case "memory_tip" -> wc.getMemoryTip();
            default -> null;
        };
    }
}
