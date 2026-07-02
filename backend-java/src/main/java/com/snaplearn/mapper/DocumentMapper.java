package com.snaplearn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snaplearn.entity.SnapDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<SnapDocument> {
}
