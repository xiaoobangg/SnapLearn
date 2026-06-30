package com.snaplearn.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.dto.request.CardCreateRequest;
import com.snaplearn.entity.Card;
import com.snaplearn.entity.CardGroup;
import com.snaplearn.entity.Word;
import com.snaplearn.mapper.CardGroupMapper;
import com.snaplearn.mapper.CardMapper;
import com.snaplearn.mapper.WordMapper;
import com.snaplearn.service.CardGroupService;
import com.snaplearn.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReactAgent 用的工具集（无状态 Spring Bean）。
 * <p>
 * userId 解析统一委托 {@link UserIdResolver}（优先 ToolContext metadata，fallback {@link AgentContext} ThreadLocal），
 * 由 CardGroupAgentService 在构建 RunnableConfig 时注入 metadata。
 * <p>
 * 设计原则：
 * <ul>
 *   <li><b>只读</b>：所有方法都不写库</li>
 *   <li><b>无状态</b>：userId 从 ToolContext metadata 读取，不存字段</li>
 *   <li><b>结构化返回</b>：返回 Map 方便 trace</li>
 *   <li><b>异常吞掉</b>：catch 后返回 {error:"..."}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardGroupAgentTools {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z\\-]{1,30}");

    private final WordMapper wordMapper;
    private final CardMapper cardMapper;
    private final CardGroupMapper cardGroupMapper;
    private final LLMService llmService;
    private final CardGroupService cardGroupService;

    /**
     * 优先从 ToolContext metadata 取 userId（Agent 模式），fallback 到 ThreadLocal（非 Agent 场景）
     */
    private String userId(ToolContext ctx) {
        return UserIdResolver.resolve(ctx);
    }

    @Tool(description = "从一段文本里提取英文单词列表，去重。输入是用户描述的原文，返回单词数组。")
    public Map<String, Object> extractWords(
            @ToolParam(description = "包含英文单词的原始文本") String text,
            ToolContext toolContext
    ) {
        if (text == null || text.isBlank()) {
            return Map.of("words", List.of());
        }
        Matcher m = WORD_PATTERN.matcher(text);
        Set<String> words = new LinkedHashSet<>();
        while (m.find()) {
            String w = m.group().toLowerCase();
            if (w.length() >= 2) words.add(w);
        }
        log.info("[AGENT-TOOL] extractWords userId={} input.len={} found={}", userId(toolContext), text.length(), words.size());
        return Map.of("words", new ArrayList<>(words));
    }

    @Tool(description = "查询用户已经学过哪些单词。输入是单词列表，返回 learned（已学过）和 newWords（未学过）两个数组。")
    public Map<String, Object> checkExistingWords(
            @ToolParam(description = "要检查的英文单词列表") List<String> words,
            ToolContext toolContext
    ) {
        if (words == null || words.isEmpty()) {
            return Map.of("learned", List.of(), "newWords", List.of());
        }
        try {
            QueryWrapper<Word> wordQw = new QueryWrapper<>();
            wordQw.in("word_text", words);
            List<Word> existingWords = wordMapper.selectList(wordQw);
            Map<String, String> wordTextToId = new HashMap<>();
            for (Word w : existingWords) {
                wordTextToId.put(w.getWordText(), w.getId());
            }
            Set<String> learnedSet = new HashSet<>();
            if (!wordTextToId.isEmpty()) {
                QueryWrapper<Card> cardQw = new QueryWrapper<>();
                cardQw.eq("user_id", userId(toolContext)).in("word_id", wordTextToId.values());
                List<Card> userCards = cardMapper.selectList(cardQw);
                Set<String> learnedWordIds = new HashSet<>();
                for (Card c : userCards) {
                    learnedWordIds.add(c.getWordId());
                }
                for (Map.Entry<String, String> e : wordTextToId.entrySet()) {
                    if (learnedWordIds.contains(e.getValue())) {
                        learnedSet.add(e.getKey());
                    }
                }
            }
            List<String> learned = new ArrayList<>(learnedSet);
            List<String> newWords = new ArrayList<>();
            for (String w : words) {
                if (!learnedSet.contains(w.toLowerCase())) {
                    newWords.add(w);
                }
            }
            log.info("[AGENT-TOOL] checkExistingWords userId={} in={} learned={} new={}",
                    userId(toolContext), words.size(), learned.size(), newWords.size());
            return Map.of("learned", learned, "newWords", newWords);
        } catch (Exception e) {
            log.error("[AGENT-TOOL] checkExistingWords failed", e);
            return Map.of("error", "查重失败：" + e.getMessage(), "learned", List.of(), "newWords", words);
        }
    }

    @Tool(description = "基于种子词推荐相关词。输入种子词数组和数量 n，返回 n 个语义相关的英文单词。")
    public Map<String, Object> recommendRelatedWords(
            @ToolParam(description = "种子单词数组") List<String> seedWords,
            @ToolParam(description = "希望推荐的数量，1-10") int n,
            ToolContext toolContext
    ) {
        if (seedWords == null || seedWords.isEmpty()) {
            return Map.of("recommendations", List.of());
        }
        int safeN = Math.max(1, Math.min(n, 10));
        try {
            String prompt = "下列单词围绕同一主题：" + String.join(", ", seedWords)
                    + "。请额外推荐 " + safeN + " 个语义相关的英文单词，每行一个，仅输出单词本身，不要编号、不要解释。";
            String toolConvId = "agt-" + UUID.randomUUID().toString().substring(0, 8);
            String resp = llmService.chatStr(prompt, "deepseek", toolConvId, userId(toolContext));
            List<String> recs = new ArrayList<>();
            if (resp != null) {
                for (String line : resp.split("\\r?\\n")) {
                    String w = line.trim().replaceAll("[^a-zA-Z\\-]", "");
                    if (w.length() >= 2 && !seedWords.contains(w.toLowerCase())) {
                        recs.add(w.toLowerCase());
                    }
                    if (recs.size() >= safeN) {
                        break;
                    }
                }
            }
            log.info("[AGENT-TOOL] recommendRelatedWords userId={} seeds={} got={}", userId(toolContext), seedWords.size(), recs.size());
            return Map.of("recommendations", recs);
        } catch (Exception e) {
            log.error("[AGENT-TOOL] recommendRelatedWords failed", e);
            return Map.of("error", "推荐失败：" + e.getMessage(), "recommendations", List.of());
        }
    }

    @Tool(description = "创建卡片组。用户确认后调用，传入标题和单词数组，真正创建卡片组并返回结果。")
    public Map<String, Object> createCardGroup(
            @ToolParam(description = "卡片组标题") String title,
            @ToolParam(description = "卡片组包含的英文单词数组") List<String> words,
            ToolContext toolContext
    ) {
        if (words == null || words.isEmpty()) {
            return Map.of("error", "单词列表不能为空");
        }
        Set<String> deduped = new LinkedHashSet<>();
        for (String w : words) {
            if (w != null && !w.isBlank()) deduped.add(w.trim().toLowerCase());
        }
        List<String> wordList = new ArrayList<>(deduped);
        String finalTitle = (title != null && !title.isBlank()) ? title : ("新卡片组 · " + wordList.size() + "词");
        log.info("[AGENT-TOOL] createCardGroup userId={} title='{}' wordCount={}", userId(toolContext), finalTitle, wordList.size());
        try {
            CardCreateRequest req = new CardCreateRequest(null, null, wordList);
            String groupId = cardGroupService.create(userId(toolContext), req).id();
            return Map.of(
                    "success", true,
                    "groupId", groupId,
                    "title", finalTitle,
                    "wordCount", wordList.size(),
                    "message", "已创建卡片组「" + finalTitle + "」，包含 " + wordList.size() + " 张卡片"
            );
        } catch (Exception e) {
            log.error("[AGENT-TOOL] createCardGroup failed userId={}", userId(toolContext), e);
            return Map.of("error", "创建失败：" + e.getMessage());
        }
    }

    @Tool(description = "撤销/删除当前用户最近一次创建的卡片组。当用户表达\"撤销\"\"删了\"\"不要了\"时调用。")
    public Map<String, Object> deleteLastCardGroup(ToolContext toolContext) {
        try {
            QueryWrapper<CardGroup> qw = new QueryWrapper<>();
            qw.eq("user_id", userId(toolContext)).orderByDesc("created_at").last("LIMIT 1");
            CardGroup latest = cardGroupMapper.selectOne(qw);
            if (latest == null) {
                return Map.of("success", false, "message", "没有可撤销的卡片组");
            }
            String title = latest.getTitle();
            cardGroupService.delete(latest.getId(), userId(toolContext));
            log.info("[AGENT-TOOL] deleteLastCardGroup userId={} groupId={} title='{}'", userId(toolContext), latest.getId(), title);
            return Map.of(
                    "success", true,
                    "groupId", latest.getId(),
                    "title", title,
                    "message", "已删除卡片组「" + title + "」"
            );
        } catch (Exception e) {
            log.error("[AGENT-TOOL] deleteLastCardGroup failed userId={}", userId(toolContext), e);
            return Map.of("error", "删除失败：" + e.getMessage());
        }
    }
}
