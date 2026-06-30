package com.snaplearn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snaplearn.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
