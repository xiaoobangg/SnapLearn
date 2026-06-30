package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.snaplearn.entity.UserSettings;
import com.snaplearn.mapper.UserSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSettingsService {

    private final UserSettingsMapper userSettingsMapper;

    public UserSettings getOrCreate(String userId) {
        QueryWrapper<UserSettings> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        UserSettings settings = userSettingsMapper.selectOne(qw);
        if (settings != null) return settings;

        settings = new UserSettings();
        settings.setId(UUID.randomUUID().toString());
        settings.setUserId(userId);
        settings.setDailyNewWords(10);
        settings.setDailyReviewWords(20);
        settings.setCheckinReminder(false);
        settings.setReminderTime(LocalTime.of(9, 0));
        userSettingsMapper.insert(settings);
        return settings;
    }

    public UserSettings update(String userId, Integer dailyNewWords, Integer dailyReviewWords,
                               Boolean checkinReminder, LocalTime reminderTime) {
        UserSettings settings = getOrCreate(userId);
        if (dailyNewWords != null) settings.setDailyNewWords(dailyNewWords);
        if (dailyReviewWords != null) settings.setDailyReviewWords(dailyReviewWords);
        if (checkinReminder != null) settings.setCheckinReminder(checkinReminder);
        if (reminderTime != null) settings.setReminderTime(reminderTime);
        userSettingsMapper.updateById(settings);
        return settings;
    }

    /** 更新用户音色偏好 */
    public void updateVoiceId(String userId, String voiceId) {
        UserSettings settings = getOrCreate(userId);
        settings.setVoiceId(voiceId);
        userSettingsMapper.updateById(settings);
    }

    /** 获取用户当前音色 ID，未设置则返回 null */
    public String getVoiceId(String userId) {
        return getOrCreate(userId).getVoiceId();
    }

    /** 获取聊天偏好 */
    public Map<String, Object> getChatPreferences(String userId) {
        UserSettings s = getOrCreate(userId);
        return Map.of(
                "chat_mode", (Object)(s.getChatMode() != null ? s.getChatMode() : "chat"),
                "chat_model", s.getChatModel() != null ? s.getChatModel() : "deepseek",
                "chat_stream", s.getChatStream() != null ? s.getChatStream() : true
        );
    }

    /** 更新聊天偏好 */
    public void updateChatPreferences(String userId, String chatMode, String chatModel, Boolean chatStream) {
        UserSettings s = getOrCreate(userId);
        if (chatMode != null) s.setChatMode(chatMode);
        if (chatModel != null) s.setChatModel(chatModel);
        if (chatStream != null) s.setChatStream(chatStream);
        userSettingsMapper.updateById(s);
    }
}
