package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyCheckinService {

    private final UserDailyPoolMapper userDailyPoolMapper;
    private final DailyCheckinLogMapper dailyCheckinLogMapper;
    private final WordBankMapper wordBankMapper;
    private final WordBankItemMapper wordBankItemMapper;
    private final WordMapper wordMapper;
    private final WordContentMapper wordContentMapper;
    private final UserSettingsMapper userSettingsMapper;

    /**
     * Get today's words for a user from a specific bank
     */
    public Map<String, Object> getTodayWords(String userId, String bankId) {
        WordBank bank = wordBankMapper.selectById(bankId);
        if (bank == null) throw new BusinessException(404, "词库不存在");

        // Get new words (not yet in user's pool)
        QueryWrapper<UserDailyPool> poolQw = new QueryWrapper<>();
        poolQw.eq("user_id", userId).eq("bank_id", bankId);
        List<UserDailyPool> existingPool = userDailyPoolMapper.selectList(poolQw);
        Set<String> existingWordIds = new HashSet<>();
        for (UserDailyPool p : existingPool) {
            existingWordIds.add(p.getWordId());
        }

        // Get user settings for daily counts
        QueryWrapper<UserSettings> settingsQw = new QueryWrapper<>();
        settingsQw.eq("user_id", userId);
        UserSettings settings = userSettingsMapper.selectOne(settingsQw);
        int newWordsLimit = settings != null && settings.getDailyNewWords() != null ? settings.getDailyNewWords() : 10;
        int reviewWordsLimit = settings != null && settings.getDailyReviewWords() != null ? settings.getDailyReviewWords() : 20;

        // Fetch new words from bank
        List<Map<String, Object>> newWords = new ArrayList<>();
        QueryWrapper<WordBankItem> itemQw = new QueryWrapper<>();
        itemQw.eq("bank_id", bankId).orderByAsc("added_at");
        List<WordBankItem> items = wordBankItemMapper.selectList(itemQw);
        for (WordBankItem item : items) {
            if (newWords.size() >= newWordsLimit) break;
            if (!existingWordIds.contains(item.getWordId())) {
                Word word = wordMapper.selectById(item.getWordId());
                if (word != null) {
                    // Add to user pool
                    UserDailyPool pool = new UserDailyPool();
                    pool.setId(UUID.randomUUID().toString());
                    pool.setUserId(userId);
                    pool.setBankId(bankId);
                    pool.setWordId(word.getId());
                    pool.setPoolStatus("new");
                    pool.setIntervalDays(0);
                    pool.setReviewCount(0);
                    pool.setNextReviewAt(LocalDateTime.now());
                    userDailyPoolMapper.insert(pool);

                    newWords.add(buildWordInfo(word, pool));
                }
            }
        }

        // Get review words (next_review_at <= now)
        List<Map<String, Object>> reviewWords = new ArrayList<>();
        QueryWrapper<UserDailyPool> reviewQw = new QueryWrapper<>();
        reviewQw.eq("user_id", userId).eq("bank_id", bankId)
                .le("next_review_at", LocalDateTime.now())
                .orderByAsc("next_review_at")
                .last("LIMIT " + reviewWordsLimit);
        List<UserDailyPool> reviewPool = userDailyPoolMapper.selectList(reviewQw);
        for (UserDailyPool p : reviewPool) {
            Word word = wordMapper.selectById(p.getWordId());
            if (word != null) {
                reviewWords.add(buildWordInfo(word, p));
            }
        }

        return Map.of(
                "bank_id", bankId,
                "bank_name", bank.getName(),
                "new_words", newWords,
                "review_words", reviewWords
        );
    }

    /**
     * Mark a word as known/fuzzy/unknown
     */
    public Map<String, Object> markWord(String poolId, String userId, String mark) {
        UserDailyPool pool = userDailyPoolMapper.selectById(poolId);
        if (pool == null || !pool.getUserId().equals(userId)) {
            throw new BusinessException(404, "记录不存在");
        }

        pool.setLastMark(mark);
        pool.setReviewCount(pool.getReviewCount() + 1);
        pool.setLastReviewAt(LocalDateTime.now());

        // Baicizhan-style scheduling
        int currentInterval = pool.getIntervalDays() != null ? pool.getIntervalDays() : 0;
        int newInterval;
        String newStatus;

        switch (mark) {
            case "known" -> {
                newInterval = currentInterval == 0 ? 1 : currentInterval * 2;
                newStatus = pool.getReviewCount() >= 5 ? "mastered" : "review";
            }
            case "fuzzy" -> {
                newInterval = Math.max(1, currentInterval);
                newStatus = "review";
            }
            case "unknown" -> {
                newInterval = 1;
                newStatus = "learning";
            }
            default -> throw new BusinessException(400, "无效的标记: " + mark);
        }

        pool.setIntervalDays(newInterval);
        pool.setNextReviewAt(LocalDateTime.now().plusDays(newInterval));
        pool.setPoolStatus(newStatus);
        userDailyPoolMapper.updateById(pool);

        return Map.of(
                "ok", true,
                "interval_days", newInterval,
                "next_review_at", pool.getNextReviewAt().toString(),
                "status", newStatus
        );
    }

    /**
     * Get monthly checkin calendar
     */
    public Map<String, Object> getCalendar(String userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);

        QueryWrapper<DailyCheckinLog> qw = new QueryWrapper<>();
        qw.eq("user_id", userId)
                .ge("checkin_date", start)
                .lt("checkin_date", end);
        List<DailyCheckinLog> logs = dailyCheckinLogMapper.selectList(qw);

        List<String> checkinDays = new ArrayList<>();
        Map<String, Object> dayStats = new LinkedHashMap<>();
        for (DailyCheckinLog log : logs) {
            String day = log.getCheckinDate().toString();
            checkinDays.add(day);
            dayStats.put(day, Map.of(
                    "new_words", log.getNewWordsCount() != null ? log.getNewWordsCount() : 0,
                    "review_words", log.getReviewWordsCount() != null ? log.getReviewWordsCount() : 0
            ));
        }

        return Map.of(
                "year", year,
                "month", month,
                "checkin_days", checkinDays,
                "total_days", checkinDays.size(),
                "day_stats", dayStats
        );
    }

    /**
     * Log a daily checkin completion
     */
    public void logCheckin(String userId, String bankId, int newCount, int reviewCount,
                           int knownCount, int fuzzyCount, int unknownCount) {
        LocalDate today = LocalDate.now();
        // Upsert: update if today's record already exists
        QueryWrapper<DailyCheckinLog> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("bank_id", bankId).eq("checkin_date", today);
        DailyCheckinLog existing = dailyCheckinLogMapper.selectOne(qw);
        if (existing != null) {
            existing.setNewWordsCount(existing.getNewWordsCount() + newCount);
            existing.setReviewWordsCount(existing.getReviewWordsCount() + reviewCount);
            existing.setKnownCount(existing.getKnownCount() + knownCount);
            existing.setFuzzyCount(existing.getFuzzyCount() + fuzzyCount);
            existing.setUnknownCount(existing.getUnknownCount() + unknownCount);
            dailyCheckinLogMapper.updateById(existing);
        } else {
            DailyCheckinLog log = new DailyCheckinLog();
            log.setId(UUID.randomUUID().toString());
            log.setUserId(userId);
            log.setBankId(bankId);
            log.setCheckinDate(today);
            log.setNewWordsCount(newCount);
            log.setReviewWordsCount(reviewCount);
            log.setKnownCount(knownCount);
            log.setFuzzyCount(fuzzyCount);
            log.setUnknownCount(unknownCount);
            dailyCheckinLogMapper.insert(log);
        }
    }

    /**
     * Get checkin stats for a user
     */
    public Map<String, Object> getStats(String userId) {
        // Total checkin days
        QueryWrapper<DailyCheckinLog> totalQw = new QueryWrapper<>();
        totalQw.eq("user_id", userId);
        long totalDays = dailyCheckinLogMapper.selectCount(totalQw);

        // Consecutive days
        int consecutiveDays = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 365; i++) {
            LocalDate date = today.minusDays(i);
            QueryWrapper<DailyCheckinLog> dayQw = new QueryWrapper<>();
            dayQw.eq("user_id", userId).eq("checkin_date", date);
            if (dailyCheckinLogMapper.selectCount(dayQw) > 0) {
                consecutiveDays++;
            } else {
                break;
            }
        }

        // Total words in pool
        QueryWrapper<UserDailyPool> poolQw = new QueryWrapper<>();
        poolQw.eq("user_id", userId);
        long totalPoolWords = userDailyPoolMapper.selectCount(poolQw);

        poolQw.eq("pool_status", "mastered");
        long masteredCount = userDailyPoolMapper.selectCount(poolQw);

        return Map.of(
                "total_checkin_days", totalDays,
                "consecutive_days", consecutiveDays,
                "total_pool_words", totalPoolWords,
                "mastered_count", masteredCount
        );
    }

    private Map<String, Object> buildWordInfo(Word word, UserDailyPool pool) {
        WordContent wc = wordContentMapper.selectOne(
                new QueryWrapper<WordContent>().eq("word_id", word.getId())
        );
        Map<String, Object> info = new HashMap<>();
        info.put("pool_id", pool.getId());
        info.put("word_id", word.getId());
        info.put("word_text", word.getWordText());
        info.put("pronunciation", wc != null ? wc.getPronunciation() : "");
        info.put("pos", wc != null ? wc.getPos() : "");
        info.put("general_meaning", wc != null ? wc.getGeneralMeaning() : "");
        info.put("extended_meaning", wc != null ? wc.getExtendedMeaning() : "");
        info.put("example_sentence", wc != null ? wc.getExampleSentence() : "");
        info.put("memory_tip", wc != null ? wc.getMemoryTip() : "");
        info.put("pool_status", pool.getPoolStatus());
        info.put("review_count", pool.getReviewCount());
        return info;
    }
}
