package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snaplearn.dto.response.NotebookResponse;
import com.snaplearn.mapper.NotebookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Deprecated // v2.0 — replaced by DailyCheckinService
@Service
@RequiredArgsConstructor
public class NotebookService {

    private final NotebookMapper notebookMapper;

    public PageResult listNotebook(String userId, String status, int page, int pageSize) {
        Page<com.snaplearn.entity.Notebook> mpPage = new Page<>(page, pageSize);
        IPage<NotebookResponse> result = notebookMapper.selectNotebookPage(mpPage, userId, status);
        return new PageResult(result.getRecords(), page, pageSize, result.getTotal());
    }

    public void removeFromNotebook(String notebookId, String userId) {
        QueryWrapper<com.snaplearn.entity.Notebook> qw = new QueryWrapper<>();
        qw.eq("id", notebookId).eq("user_id", userId);
        notebookMapper.delete(qw);
    }

    public record PageResult(
            java.util.List<NotebookResponse> items,
            int page,
            int pageSize,
            long total
    ) {
    }
}
