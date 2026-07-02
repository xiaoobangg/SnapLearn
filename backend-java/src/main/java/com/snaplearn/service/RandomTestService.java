package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RandomTestService {

    private final RandomTestPoolMapper poolMapper;
    private final TestQuestionMapper testQuestionMapper;
    private final TestAttemptMapper testAttemptMapper;
    private final WordMapper wordMapper;
    private final DailyCheckinService dailyCheckinService;

    private static final String RANDOM_BANK_ID = "random-test-bank";
    private static final List<String> QUESTION_TYPES = List.of("meaning_select", "word_select", "collocation", "spelling");

    /** Get random entries from pool, fetch matching questions, assemble a test session */
    public Map<String, Object> start(String userId, int count) {
        QueryWrapper<RandomTestPool> pqw = new QueryWrapper<>();
        int limit = Math.min(count, 10);
        pqw.eq("user_id", userId).gt("review_count", 0).last("ORDER BY RANDOM() LIMIT " + limit);
        List<RandomTestPool> pool = poolMapper.selectList(pqw);

        if (pool.isEmpty()) {
            return Map.of("ok", false, "message", "随机测试池为空，完成更多测试题后会自动填充");
        }

        // Match pool entries to questions: (word_id, question_type) → TestQuestion
        List<TestQuestion> questions = new ArrayList<>();
        for (RandomTestPool p : pool) {
            QueryWrapper<TestQuestion> tqw = new QueryWrapper<>();
            tqw.eq("word_id", p.getWordId()).eq("question_type", p.getQuestionType());
            TestQuestion q = testQuestionMapper.selectOne(tqw);
            if (q != null) questions.add(q);
        }

        List<String> wordIds = pool.stream().map(RandomTestPool::getWordId).distinct().toList();
        List<Word> words = wordMapper.selectBatchIds(wordIds);
        Map<String, String> wordTextMap = new HashMap<>();
        words.forEach(w -> wordTextMap.put(w.getId(), w.getWordText()));

        List<Map<String, Object>> shuffled = new ArrayList<>();
        for (TestQuestion q : questions) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("word_id", q.getWordId());
            m.put("word", wordTextMap.getOrDefault(q.getWordId(), ""));
            m.put("question_type", q.getQuestionType());
            m.put("question_text", q.getQuestionText());
            m.put("options", shuffleOptions(q.getOptions()));
            m.put("correct_answer", q.getCorrectAnswer());
            shuffled.add(m);
        }
        Collections.shuffle(shuffled);

        return Map.of("ok", true, "questions", shuffled, "total", shuffled.size(),
                "source", "random");
    }

    @Transactional
    public Map<String, Object> submit(String userId, List<String> questionIds,
                                       List<String> userAnswers) {
        int correctCount = 0;
        for (int i = 0; i < questionIds.size(); i++) {
            String qid = questionIds.get(i);
            String answer = i < userAnswers.size() ? userAnswers.get(i) : "";
            TestQuestion q = testQuestionMapper.selectById(qid);
            boolean ok = q != null && q.getCorrectAnswer().equals(answer);

            TestAttempt att = new TestAttempt();
            att.setId(UUID.randomUUID().toString());
            att.setQuestionId(qid);
            att.setUserId(userId);
            att.setUserAnswer(answer);
            att.setIsCorrect(ok);
            testAttemptMapper.insert(att);

            if (ok) correctCount++;

            // Update pool per question
            if (q != null) {
                if (ok) {
                    decrement(userId, q.getWordId(), q.getQuestionType());
                } else {
                    upsert(userId, q.getWordId(), q.getQuestionType(), 4, "error");
                }
            }
        }
        int total = questionIds.size();
        // Log daily check-in: all words are review, correct=known, wrong=unknown
        int wrongCount = total - correctCount;
        dailyCheckinService.logCheckin(userId, RANDOM_BANK_ID, 0, total, correctCount, 0, wrongCount);
        return Map.of("total", total, "correct", correctCount, "all_correct", total > 0 && correctCount == total);
    }

    /** Called by frontend immediately when user selects wrong answer */
    @Transactional
    public void markWrong(String userId, String wordId, String questionType) {
        upsert(userId, wordId, questionType, 4, "error");
    }

    /** Called when test questions are first generated for a word — all 4 types enter pool */
    public void onFirstGenerate(String userId, String wordId) {
        for (String type : QUESTION_TYPES) {
            QueryWrapper<RandomTestPool> qw = new QueryWrapper<>();
            qw.eq("user_id", userId).eq("word_id", wordId).eq("question_type", type);
            if (poolMapper.selectCount(qw) == 0) {
                upsert(userId, wordId, type, 2, "auto");
            }
        }
    }

    private void upsert(String userId, String wordId, String questionType, int count, String source) {
        QueryWrapper<RandomTestPool> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("word_id", wordId).eq("question_type", questionType);
        RandomTestPool existing = poolMapper.selectOne(qw);
        if (existing != null) {
            existing.setReviewCount(count);
            existing.setSource(source);
            poolMapper.updateById(existing);
        } else {
            RandomTestPool p = new RandomTestPool();
            p.setId(UUID.randomUUID().toString());
            p.setUserId(userId);
            p.setWordId(wordId);
            p.setQuestionType(questionType);
            p.setReviewCount(count);
            p.setSource(source);
            poolMapper.insert(p);
        }
    }

    private void decrement(String userId, String wordId, String questionType) {
        QueryWrapper<RandomTestPool> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("word_id", wordId).eq("question_type", questionType);
        RandomTestPool existing = poolMapper.selectOne(qw);
        if (existing != null) {
            int c = existing.getReviewCount() - 1;
            if (c <= 0) poolMapper.deleteById(existing.getId());
            else { existing.setReviewCount(c); poolMapper.updateById(existing); }
        }
    }

    private String shuffleOptions(String json) {
        if (json == null || json.isBlank()) return "[]";
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> opts = om.readValue(json, List.class);
            Collections.shuffle(opts);
            return om.writeValueAsString(opts);
        } catch (Exception e) { return json; }
    }
}
