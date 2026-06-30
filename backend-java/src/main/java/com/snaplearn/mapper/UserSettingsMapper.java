package com.snaplearn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snaplearn.entity.UserSettings;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {
}
