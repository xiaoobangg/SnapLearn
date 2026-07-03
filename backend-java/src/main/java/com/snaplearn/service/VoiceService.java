package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.snaplearn.entity.Voice;
import com.snaplearn.entity.WordAudio;
import com.snaplearn.mapper.VoiceMapper;
import com.snaplearn.mapper.WordAudioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final VoiceMapper voiceMapper;
    private final WordAudioMapper wordAudioMapper;

    public List<Voice> listActive() {
        QueryWrapper<Voice> qw = new QueryWrapper<>();
        qw.eq("is_active", true).orderByAsc("name");
        return voiceMapper.selectList(qw);
    }

    public List<Voice> listAll() {
        QueryWrapper<Voice> qw = new QueryWrapper<>();
        qw.orderByAsc("name");
        return voiceMapper.selectList(qw);
    }

    public Voice create(Voice v) {
        v.setId(UUID.randomUUID().toString());
        voiceMapper.insert(v);
        return v;
    }

    public void update(Voice v) {
        voiceMapper.updateById(v);
    }

    public void delete(String id) {
        // 先清关联的音频记录
        QueryWrapper<WordAudio> aq = new QueryWrapper<>();
        aq.eq("voice_id", id);
        wordAudioMapper.delete(aq);
        voiceMapper.deleteById(id);
    }

    public void deleteByCode(String voiceCode) {
        Voice v = voiceMapper.selectOne(new QueryWrapper<Voice>().eq("voice_code", voiceCode));
        if (v != null) {
            delete(v.getId());
        }
    }

    /**
     * 设为唯一默认音色，其他音色全部降级
     */
    public void setAsDefault(String id) {
        UpdateWrapper<Voice> clear = new UpdateWrapper<>();
        clear.set("is_default", false);
        voiceMapper.update(null, clear);

        Voice v = new Voice();
        v.setId(id);
        v.setIsDefault(true);
        voiceMapper.updateById(v);
    }

    /**
     * 当前默认音色，无默认则取第一个启用的
     */
    public Voice getDefault() {
        QueryWrapper<Voice> qw = new QueryWrapper<>();
        qw.eq("is_default", true).eq("is_active", true);
        Voice v = voiceMapper.selectOne(qw);
        if (v == null) {
            // fallback: 取第一个启用的
            QueryWrapper<Voice> fw = new QueryWrapper<>();
            fw.eq("is_active", true).orderByAsc("name").last("LIMIT 1");
            v = voiceMapper.selectOne(fw);
        }
        return v;
    }

    /**
     * 当前用户生效音色
     */
    public Voice getEffectiveVoice(String voiceIdFromSettings) {
        if (voiceIdFromSettings != null) {
            Voice v = voiceMapper.selectById(voiceIdFromSettings);
            if (v != null && Boolean.TRUE.equals(v.getIsActive())) return v;
        }
        return getDefault();
    }
}
