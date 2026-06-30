package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMapper userMapper;
    private final CardGroupMapper cardGroupMapper;
    private final CardMapper cardMapper;
    private final UserDailyPoolMapper userDailyPoolMapper;
    private final DailyCheckinLogMapper dailyCheckinLogMapper;

    public Map<String, Object> getOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalUsers = userMapper.selectCount(new QueryWrapper<>());
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNewUsers = userMapper.selectCount(
                new QueryWrapper<User>().ge("created_at", todayStart));
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(7).atStartOfDay();
        long activeUsers7d = userDailyPoolMapper.selectCount(
                new QueryWrapper<UserDailyPool>()
                        .ge("last_review_at", sevenDaysAgo)
                        .select("DISTINCT user_id"));

        long totalGroups = cardGroupMapper.selectCount(new QueryWrapper<>());
        long totalCards = cardMapper.selectCount(new QueryWrapper<>());
        long todayNewCards = cardMapper.selectCount(
                new QueryWrapper<Card>().ge("created_at", todayStart));

        long todayReviews = userDailyPoolMapper.selectCount(
                new QueryWrapper<UserDailyPool>().ge("last_review_at", todayStart));
        long totalPool = userDailyPoolMapper.selectCount(new QueryWrapper<>());
        long masteredCount = userDailyPoolMapper.selectCount(
                new QueryWrapper<UserDailyPool>().eq("pool_status", "mastered"));

        long todayReviewDue = userDailyPoolMapper.selectCount(
                new QueryWrapper<UserDailyPool>().le("next_review_at", LocalDateTime.now()));

        // Checkin stats
        long todayCheckins = dailyCheckinLogMapper.selectCount(
                new QueryWrapper<DailyCheckinLog>().eq("checkin_date", LocalDate.now()));

        stats.put("total_users", totalUsers);
        stats.put("today_new_users", todayNewUsers);
        stats.put("active_users_7d", activeUsers7d);
        stats.put("total_groups", totalGroups);
        stats.put("total_cards", totalCards);
        stats.put("today_new_cards", todayNewCards);
        stats.put("today_reviews", todayReviews);
        stats.put("total_pool", totalPool);
        stats.put("mastered_count", masteredCount);
        stats.put("today_review_due", todayReviewDue);
        stats.put("today_checkins", todayCheckins);
        stats.put("mastered_rate", totalPool > 0
                ? Math.round(masteredCount * 10000.0 / totalPool) / 100.0 : 0);

        // Group status distribution
        QueryWrapper<CardGroup> pendingQw = new QueryWrapper<CardGroup>().eq("group_status", "pending");
        QueryWrapper<CardGroup> learningQw = new QueryWrapper<CardGroup>().eq("group_status", "learning");
        QueryWrapper<CardGroup> learnDoneQw = new QueryWrapper<CardGroup>().eq("group_status", "learn_done");
        QueryWrapper<CardGroup> testingQw = new QueryWrapper<CardGroup>().eq("group_status", "testing");
        QueryWrapper<CardGroup> testDoneQw = new QueryWrapper<CardGroup>().eq("group_status", "test_done");

        stats.put("groups_pending", cardGroupMapper.selectCount(pendingQw));
        stats.put("groups_learning", cardGroupMapper.selectCount(learningQw));
        stats.put("groups_learn_done", cardGroupMapper.selectCount(learnDoneQw));
        stats.put("groups_testing", cardGroupMapper.selectCount(testingQw));
        stats.put("groups_test_done", cardGroupMapper.selectCount(testDoneQw));

        return stats;
    }

    public List<Map<String, Object>> getDailyStats(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            long newUsers = userMapper.selectCount(
                    new QueryWrapper<User>().between("created_at", dayStart, dayEnd));
            long newCards = cardMapper.selectCount(
                    new QueryWrapper<Card>().between("created_at", dayStart, dayEnd));
            long reviews = userDailyPoolMapper.selectCount(
                    new QueryWrapper<UserDailyPool>().between("last_review_at", dayStart, dayEnd));
            long newGroups = cardGroupMapper.selectCount(
                    new QueryWrapper<CardGroup>().between("created_at", dayStart, dayEnd));
            long checkins = dailyCheckinLogMapper.selectCount(
                    new QueryWrapper<DailyCheckinLog>().eq("checkin_date", date));

            Map<String, Object> dayStats = new LinkedHashMap<>();
            dayStats.put("date", date.format(fmt));
            dayStats.put("new_users", newUsers);
            dayStats.put("new_cards", newCards);
            dayStats.put("reviews", reviews);
            dayStats.put("new_groups", newGroups);
            dayStats.put("checkins", checkins);
            result.add(dayStats);
        }
        return result;
    }
}
