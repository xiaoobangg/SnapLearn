package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
@Transactional
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

    // 并行调用 LLM 的线程池
    private static final ExecutorService TEST_EXECUTOR = Executors.newFixedThreadPool(2);

    public List<TestQuestion> generateTest(String groupId, String userId) {
        CardGroup group = cardGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException(404, "卡片组不存在");
        }
        if (!"learn_done".equals(group.getGroupStatus()) && !"testing".equals(group.getGroupStatus())) {
            throw new BusinessException(400, "只有学习完成的卡片组才能测试");
        }

        // 已有题目则直接复用，不再调 LLM
        QueryWrapper<TestQuestion> existQw = new QueryWrapper<>();
        existQw.eq("group_id", groupId).orderByAsc("sort_order");
        List<TestQuestion> existing = testQuestionMapper.selectList(existQw);
        if (!existing.isEmpty()) {
            return existing;
        }

        QueryWrapper<TestQuestion> delQw = new QueryWrapper<>();
        delQw.eq("group_id", groupId);
        testQuestionMapper.delete(delQw);

        QueryWrapper<Card> cardQw = new QueryWrapper<>();
        cardQw.eq("group_id", groupId).orderByAsc("sort_order");
        List<Card> cards = cardMapper.selectList(cardQw);
        if (cards.isEmpty()) {
            throw new BusinessException(400, "卡片组中没有卡片");
        }

        // 1. 主线程收集卡片数据：每个单词 × 4 种题型
        record CardData(Card card, Word word, WordContent wc, String type, int order) {
        }
        List<CardData> cardDataList = new ArrayList<>();
        int order = 0;
        for (Card card : cards) {
            Word word = wordMapper.selectById(card.getWordId());
            if (word == null) {
                continue;
            }
            WordContent wc = wordContentMapper.selectById(card.getWordId());
            for (String type : QUESTION_TYPES) {
                cardDataList.add(new CardData(card, word, wc, type, order++));
            }
        }

        // 2. 并行调用 LLM 生成题目（不操作数据库）
        List<CompletableFuture<TestQuestion>> futures = new ArrayList<>();
        for (CardData cd : cardDataList) {
            futures.add(CompletableFuture.supplyAsync(() -> generateQuestion(cd.type, cd.card, cd.word, cd.wc, groupId, cd.order), TEST_EXECUTOR));
        }

        // 3. 等待全部完成，统一插入数据库
        List<TestQuestion> questions = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        for (TestQuestion q : questions) {
            testQuestionMapper.insert(q);
        }

        group.setGroupStatus("testing");
        cardGroupMapper.updateById(group);

        return questions;
    }

    private TestQuestion generateQuestion(String type, Card card, Word word, WordContent wc, String groupId, int order) {
        String wordText = word.getWordText();
        String prompt = buildQuestionPrompt(type, wordText, wc);
        Exception lastException = null;
        ChatClient currentClient = ChatClient.builder(deepSeekChatModel).build();
        String currentModelName = "DeepSeek";

        // 前 MAX_RETRY_TIMES 次：使用 DeepSeek 重试
        for (int attempt = 1; attempt <= Constant.MAX_RETRY_TIMES; attempt++) {
            try {
                return doGenerateQuestion(currentClient, prompt, type, card, word, wc, groupId, order);
            } catch (Exception e) {
                lastException = e;
                log.warn("[{}] 生成题目失败，单词：{}，第{}次重试", currentModelName, wordText, attempt);

                if (attempt < Constant.MAX_RETRY_TIMES) {
                    sleepQuietly(Constant.RETRY_SLEEP_MS);
                }
            }
        }

        // 三次 DeepSeek 全部失败 → 切换 通义千问 兜底执行一次
        log.warn("DeepSeek 重试{}次全部失败，切换至 DashScope(通义千问) 兜底生成，单词：{}", Constant.MAX_RETRY_TIMES, wordText);
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
     * 静默休眠，不向上抛出中断异常
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("线程休眠被中断");
        }
    }

    /**
     * 执行单次题目生成 + JSON 解析 + 实体封装
     */
    private TestQuestion doGenerateQuestion(ChatClient chatClient, String prompt, String type, Card card, Word word, WordContent wc, String groupId, int order) throws Exception {
        // 调用 LLM
        String llmResponse = chatClient.prompt().user(prompt).call().content();

        // 解析 JSON
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
        return testQuestionMapper.selectList(qw);
    }

    public Map<String, Object> submitAnswers(String groupId, String userId, List<AnswerItem> answers) {
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

        if (allCorrect) {
            group.setGroupStatus("test_done");
            cardGroupMapper.updateById(group);
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

    public record AnswerItem(String questionId, String userAnswer) {
    }
}