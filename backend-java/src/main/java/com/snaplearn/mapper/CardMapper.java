package com.snaplearn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snaplearn.entity.Card;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CardMapper extends BaseMapper<Card> {
}
