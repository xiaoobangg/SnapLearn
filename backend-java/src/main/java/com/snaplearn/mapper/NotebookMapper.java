package com.snaplearn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snaplearn.dto.response.NotebookResponse;
import com.snaplearn.dto.response.ReviewCardResponse;
import com.snaplearn.entity.Notebook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotebookMapper extends BaseMapper<Notebook> {

    IPage<NotebookResponse> selectNotebookPage(
            Page<Notebook> page,
            @Param("userId") String userId,
            @Param("status") String status
    );

    List<ReviewCardResponse> selectTodayReviewCards(@Param("userId") String userId);

    @Select("SELECT COUNT(*) FROM notebook WHERE user_id = #{userId} AND status = 'mastered' AND card_id IN (SELECT id FROM cards WHERE group_id = #{groupId})")
    Long countMasteredByGroup(@Param("groupId") String groupId, @Param("userId") String userId);

    @Select("SELECT COUNT(*) FROM notebook WHERE user_id = #{userId} AND repetitions > 0 AND card_id IN (SELECT id FROM cards WHERE group_id = #{groupId})")
    Long countReviewedByGroup(@Param("groupId") String groupId, @Param("userId") String userId);
}
