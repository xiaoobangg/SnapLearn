package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.common.utils.Constant;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.*;
import com.snaplearn.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final TestQuestionMapper testQuestionMapper;
    private final TestAttemptMapper testAttemptMapper;
    private final CardMapper cardMapper;
    private final CardGroupMapper cardGroupMapper;
    private final WordMapper wordMapper;
    private final WordContentMapper wordContentMapper;
    private final ErrorBookService errorBookService;
    private final ObjectMapper objectMapper;
    private final ChatModel deepSeekChatModel;
    private final ChatModel dashScopeChatModel;
    private final PromptLoader promptLoader;

    private static final List<String> QUESTION_TYPES = List.of("meaning_select", "word_select", "collocation", "spelling");

    // 后台生成编排线程（外层 generateAsync）
    private static final ExecutorService TEST_GEN_EXECUTOR = Executors.newFixedThreadPool(1);
    // 并行调用 LLM 的线程池（内层 generateQuestion）
    private static final ExecutorService TEST_LLM_EXECUTOR = Executors.newFixedThreadPool(1);

    /**
     * 测验入口：已有题目直接返回，无题目则 CAS 设 generating 并启动后台生成。
     */
    @Transactional
    public Map<String, Object> startTest(String groupId, String userId) {
        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片组不存在");
        }

        // 已有题目 → 直接返回
        QueryWrapper<TestQuestion> existQw = new QueryWrapper<>();
        existQw.eq("group_id", groupId).orderByAsc("sort_order");
        List<TestQuestion> existing = testQuestionMapper.selectList(existQw);
        if (!existing.isEmpty()) {
            return Map.of("status", "ready", "questions", shuffleQuestionsAndOptions(existing), "total", existing.size());
        }

        // 正在生成中 → 返回 generating
        if ("generating".equals(group.getGroupStatus())) {
            return Map.of("status", "generating");
        }

        // 状态检查：只有 learn_done 允许进入生成
        if (!"learn_done".equals(group.getGroupStatus())) {
            throw new BusinessException(400, "只有学习完成的卡片组才能测试");
        }

        // CAS：原子抢占生成权
        UpdateWrapper<CardGroup> uw = new UpdateWrapper<>();
        uw.eq("id", groupId).eq("group_status", "learn_done");
        uw.set("group_status", "generating");
        int rows = cardGroupMapper.update(null, uw);
        if (rows == 0) {
            // 没抢到——重新查状态
            group = cardGroupMapper.selectById(groupId);
            if ("generating".equals(group.getGroupStatus())) {
                return Map.of("status", "generating");
            }
            // 可能被其他请求抢到并已完成
            existing = testQuestionMapper.selectList(existQw);
            if (!existing.isEmpty()) {
                return Map.of("status", "ready", "questions", shuffleQuestionsAndOptions(existing), "total", existing.size());
            }
            throw new BusinessException(400, "当前状态不可测试");
        }

        // 抢到 → 后台异步生成
        CompletableFuture.runAsync(() -> generateAsync(groupId), TEST_GEN_EXECUTOR);
        return Map.of("status", "generating");
    }

    /** 后台 LLM 生成 → 插题 → 改状态 */
    private void generateAsync(String groupId) {
        try {
            log.info("[TEST-GEN] 开始后台生成 groupId={}", groupId);

            QueryWrapper<TestQuestion> delQw = new QueryWrapper<>();
            delQw.eq("group_id", groupId);
            testQuestionMapper.delete(delQw);

            QueryWrapper<Card> cardQw = new QueryWrapper<>();
            cardQw.eq("group_id", groupId).orderByAsc("sort_order");
            List<Card> cards = cardMapper.selectList(cardQw);
            if (cards.isEmpty()) {
                resetGenStatus(groupId);
                return;
            }

            record CardData(Card card, Word word, WordContent wc, String type, int order) {}
            List<CardData> cardDataList = new ArrayList<>();
            int order = 0;
            for (Card card : cards) {
                Word word = wordMapper.selectById(card.getWordId());
                if (word == null) continue;
                WordContent wc = wordContentMapper.selectById(card.getWordId());
                for (String type : QUESTION_TYPES) {
                    cardDataList.add(new CardData(card, word, wc, type, order++));
                }
            }

            List<CompletableFuture<TestQuestion>> futures = new ArrayList<>();
            for (CardData cd : cardDataList) {
                futures.add(CompletableFuture.supplyAsync(() -> generateQuestion(cd.type, cd.card, cd.word, cd.wc, groupId, cd.order), TEST_LLM_EXECUTOR));
            }

            List<TestQuestion> questions = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            for (TestQuestion q : questions) {
                testQuestionMapper.insert(q);
            }

            CardGroup group = cardGroupMapper.selectById(groupId);
            if (group != null && "generating".equals(group.getGroupStatus())) {
                group.setGroupStatus("testing");
                cardGroupMapper.updateById(group);
            }
            log.info("[TEST-GEN] 完成 groupId={} count={}", groupId, questions.size());
        } catch (Exception e) {
            log.error("[TEST-GEN] 失败 groupId={}", groupId, e);
            resetGenStatus(groupId);
        }
    }

    private void resetGenStatus(String groupId) {
        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group != null && "generating".equals(group.getGroupStatus())) {
            group.setGroupStatus("learn_done");
            cardGroupMapper.updateById(group);
        }
    }

    /** 状态查询（前端轮询） */
    public Map<String, Object> getTestStatus(String groupId, String userId) {
        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片组不存在");
        }
        String status = group.getGroupStatus();
        if ("testing".equals(status) || "test_done".equals(status)) {
            QueryWrapper<TestQuestion> qw = new QueryWrapper<>();
            qw.eq("group_id", groupId).orderByAsc("sort_order");
            List<TestQuestion> questions = testQuestionMapper.selectList(qw);
            return Map.of("status", status, "questions", shuffleQuestionsAndOptions(questions), "total", questions.size());
        }
        return Map.of("status", status);
    }

    private TestQuestion generateQuestion(String type, Card card, Word word, WordContent wc, String groupId, int order) {
        String wordText = word.getWordText();
        String prompt = buildQuestionPrompt(type, wordText, wc);
        Exception lastException = null;
        ChatClient currentClient = ChatClient.builder(deepSeekChatModel).build();

        // DeepSeek 尝试 1 次（测验题长文本场景容易慢，不反复重试）
        try {
            return doGenerateQuestion(currentClient, prompt, type, card, word, wc, groupId, order);
        } catch (Exception e) {
            log.warn("[DeepSeek] 生成题目失败，单词：{}，直接切换 DashScope 兜底", wordText);
        }

        // DeepSeek 失败 → 切换通义千问兜底
        log.warn("切换至 DashScope(通义千问) 兜底生成，单词：{}", wordText);
        try {
            currentClient = ChatClient.builder(dashScopeChatModel).build();
            return doGenerateQuestion(currentClient, prompt, type, card, word, wc, groupId, order);
        } catch (Exception e) {
            lastException = e;
            log.error("[{}] 兜底生成题目也失败，单词：{}", "DashScope", wordText, lastException);
            throw new BusinessException(502, "LLM 生成测试题失败（" + wordText + "）");
        }
    }

    /**
     * 执行单次题目生成 + JSON 解析 + 实体封装
     */
    private TestQuestion doGenerateQuestion(ChatClient chatClient, String prompt, String type, Card card, Word word, WordContent wc, String groupId, int order) throws Exception {
        long start = System.currentTimeMillis();
        String wordText = word.getWordText();
        log.info("[TEST-LLM] PROMPT word={} type={}: {}", wordText, type, prompt);

        String llmResponse;
        try {
            llmResponse = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[TEST-LLM] 调用失败 word={} type={} duration={}ms error={} PROMPT={}",
                    wordText, type, duration, e.toString(), prompt);
            throw e;
        }
        long duration = System.currentTimeMillis() - start;
        log.info("[TEST-LLM] RESPONSE word={} type={} duration={}ms: {}", wordText, type, duration, llmResponse);
        JsonNode node = objectMapper.readTree(llmResponse);
        String questionText = node.path("question").asText("");
        String answer = node.path("answer").asText("");

        // 基础校验
        if (questionText.isBlank() || answer.isBlank()) {
            throw new BusinessException(502, "LLM 返回内容不完整");
        }

        // 封装返回实体
        TestQuestion question = new TestQuestion();
        question.setId(UUID.randomUUID().toString());
        question.setGroupId(groupId);
        question.setCardId(card.getId());
        question.setQuestionType(type);
        question.setQuestionText(questionText);
        question.setOptions(node.path("options").toString());
        question.setCorrectAnswer(answer);
        question.setSortOrder(order);

        return question;
    }

    private String buildQuestionPrompt(String type, String wordText, WordContent wc) {
        String meaning = wc != null ? wc.getGeneralMeaning() : "";
        String firstLetter = !wordText.isEmpty() ? wordText.substring(0, 1) : "";
        String templateName = switch (type) {
            case "meaning_select" -> "test-meaning-select.st";
            case "word_select" -> "test-word-select.st";
            case "collocation" -> "test-collocation.st";
            case "spelling" -> "test-spelling.st";
            default -> throw new BusinessException(400, "未知题型: " + type);
        };
        return promptLoader.load(templateName).replace("{word}", wordText).replace("{meaning}", meaning).replace("{firstLetter}", firstLetter);
    }

    public List<TestQuestion> getExistingQuestions(String groupId, String userId) {
        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片组不存在");
        }
        if (!"testing".equals(group.getGroupStatus()) && !"learn_done".equals(group.getGroupStatus())) {
            throw new BusinessException(400, "当前状态不可重测");
        }

        QueryWrapper<TestQuestion> qw = new QueryWrapper<>();
        qw.eq("group_id", groupId).orderByAsc("sort_order");
        return shuffleQuestionsAndOptions(testQuestionMapper.selectList(qw));
    }

    /**
     * 加载测试题给前端时：打乱题目顺序 + 打乱每题选项顺序。
     * 注意：correct_answer 存的是选项的值而非索引，打乱 options 数组顺序不影响正误校验。
     * 生成题目时保持 sort_order 不变，仅此处查询返回前打乱，DB 数据不动。
     */
    private List<TestQuestion> shuffleQuestionsAndOptions(List<TestQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return questions;
        }
        List<TestQuestion> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled);
        for (TestQuestion q : shuffled) {
            try {
                JsonNode node = objectMapper.readTree(q.getOptions());
                if (node != null && node.isArray() && node.size() > 1) {
                    List<String> opts = new ArrayList<>();
                    node.forEach(o -> opts.add(o.isTextual() ? o.asText() : o.toString()));
                    Collections.shuffle(opts);
                    q.setOptions(objectMapper.writeValueAsString(opts));
                }
            } catch (Exception e) {
                log.warn("打乱选项失败 questionId={}: {}", q.getId(), e.getMessage());
            }
        }
        return shuffled;
    }

    @Transactional
    public Map<String, Object> submitAnswers(String groupId, String userId, List<AnswerItem> answers) {
        log.info("[TEST-SUBMIT] 入参 groupId={} userId={} answerCount={}", groupId, userId, answers != null ? answers.size() : 0);
        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片组不存在");
        }

        QueryWrapper<TestAttempt> maxRoundQw = new QueryWrapper<>();
        maxRoundQw.eq("user_id", userId).orderByDesc("attempt_round").last("LIMIT 1");
        TestAttempt lastAttempt = testAttemptMapper.selectOne(maxRoundQw);
        int round = (lastAttempt != null ? lastAttempt.getAttemptRound() : 0) + 1;

        int correctCount = 0;
        List<String> errorCardIds = new ArrayList<>();

        for (AnswerItem ans : answers) {
            TestQuestion question = testQuestionMapper.selectById(ans.questionId());
            if (question == null) {
                continue;
            }

            boolean isCorrect = question.getCorrectAnswer().equals(ans.userAnswer());

            TestAttempt attempt = new TestAttempt();
            attempt.setId(UUID.randomUUID().toString());
            attempt.setQuestionId(ans.questionId());
            attempt.setUserId(userId);
            attempt.setUserAnswer(ans.userAnswer());
            attempt.setIsCorrect(isCorrect);
            attempt.setAttemptRound(round);
            testAttemptMapper.insert(attempt);

            if (isCorrect) {
                correctCount++;
            } else {
                errorCardIds.add(question.getCardId());
                errorBookService.addError(groupId, question.getCardId(), userId, attempt.getId());
            }
        }

        int total = answers.size();
        boolean allCorrect = total > 0 && correctCount == total;

        log.info("[TEST-GEN] submitAnswers groupId={} total={} correct={} allCorrect={}",
                groupId, total, correctCount, allCorrect);

        if (allCorrect) {
            group.setGroupStatus("test_done");
            cardGroupMapper.updateById(group);
            log.info("[TEST-GEN] groupId={} 状态已更新为 test_done", groupId);
        }

        return Map.of("total", total, "correct", correctCount, "all_correct", allCorrect, "round", round, "error_card_ids", errorCardIds);
    }

    public Map<String, Object> getResult(String groupId, String userId) {
        QueryWrapper<TestQuestion> qw = new QueryWrapper<>();
        qw.eq("group_id", groupId).orderByAsc("sort_order");
        List<TestQuestion> questions = testQuestionMapper.selectList(qw);

        List<Map<String, Object>> items = new ArrayList<>();
        int correct = 0;
        for (TestQuestion q : questions) {
            QueryWrapper<TestAttempt> attQw = new QueryWrapper<>();
            attQw.eq("question_id", q.getId()).eq("user_id", userId).orderByDesc("created_at").last("LIMIT 1");
            TestAttempt att = testAttemptMapper.selectOne(attQw);
            boolean ok = att != null && Boolean.TRUE.equals(att.getIsCorrect());
            if (ok) {
                correct++;
            }

            Card card = cardMapper.selectById(q.getCardId());
            Word word = card != null ? wordMapper.selectById(card.getWordId()) : null;
            items.add(Map.of("question_id", q.getId(), "card_id", q.getCardId(), "word", word != null ? word.getWordText() : "", "type", q.getQuestionType(), "question", q.getQuestionText(), "options", q.getOptions(), "correct_answer", q.getCorrectAnswer(), "user_answer", att != null ? att.getUserAnswer() : "", "is_correct", ok));
        }

        CardGroup group = cardGroupMapper.selectById(groupId);
        return Map.of("questions", items, "total", questions.size(), "correct", correct, "all_correct", correct == questions.size(), "group_status", group != null ? group.getGroupStatus() : "");
    }

    /** 调试入口：直接传 prompt，省去拼装逻辑 */
    public Map<String, Object> debugGenerate(String prompt, String model) {
        ChatClient client = "dashscope".equalsIgnoreCase(model)
                ? ChatClient.builder(dashScopeChatModel).build()
                : ChatClient.builder(deepSeekChatModel).build();

        long start = System.currentTimeMillis();
        try {
            String llmResponse = client.prompt().user(prompt).call().content();
            long duration = System.currentTimeMillis() - start;
            return Map.of(
                    "success", true,
                    "model", model,
                    "durationMs", duration,
                    "response", llmResponse
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "model", model,
                    "durationMs", System.currentTimeMillis() - start,
                    "error", e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        }
    }

    public record AnswerItem(@com.fasterxml.jackson.annotation.JsonProperty("questionId") String questionId, @com.fasterxml.jackson.annotation.JsonProperty("userAnswer") String userAnswer) {
    }
}