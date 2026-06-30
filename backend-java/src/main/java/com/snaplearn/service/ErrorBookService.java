package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.snaplearn.entity.ErrorBook;
import com.snaplearn.mapper.ErrorBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ErrorBookService {

    private final ErrorBookMapper errorBookMapper;

    public void addError(String groupId, String cardId, String userId, String testAttemptId) {
        ErrorBook eb = new ErrorBook();
        eb.setId(UUID.randomUUID().toString());
        eb.setGroupId(groupId);
        eb.setCardId(cardId);
        eb.setUserId(userId);
        eb.setTestAttemptId(testAttemptId);
        eb.setResolved(false);
        errorBookMapper.insert(eb);
    }

    public List<ErrorBook> getByGroup(String groupId, String userId) {
        QueryWrapper<ErrorBook> qw = new QueryWrapper<>();
        qw.eq("group_id", groupId).eq("user_id", userId).orderByDesc("created_at");
        return errorBookMapper.selectList(qw);
    }

    public List<ErrorBook> getUnresolvedByGroup(String groupId, String userId) {
        QueryWrapper<ErrorBook> qw = new QueryWrapper<>();
        qw.eq("group_id", groupId).eq("user_id", userId).eq("resolved", false);
        return errorBookMapper.selectList(qw);
    }

    public void markResolved(String errorBookId) {
        UpdateWrapper<ErrorBook> uw = new UpdateWrapper<>();
        uw.eq("id", errorBookId).set("resolved", true);
        errorBookMapper.update(null, uw);
    }

    public void markResolvedByCard(String groupId, String cardId, String userId) {
        UpdateWrapper<ErrorBook> uw = new UpdateWrapper<>();
        uw.eq("group_id", groupId).eq("card_id", cardId).eq("user_id", userId)
                .set("resolved", true);
        errorBookMapper.update(null, uw);
    }
}
