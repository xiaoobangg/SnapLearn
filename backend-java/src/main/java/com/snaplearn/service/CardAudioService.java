package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.Voice;
import com.snaplearn.entity.WordAudio;
import com.snaplearn.mapper.WordAudioMapper;
import com.snaplearn.service.tts.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 单词音频服务（已从 snap_card_audios 迁移到 snap_word_audios）。
 * <br>异步生成入口：{@link CardAudioAsyncService}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardAudioService {

    private final WordAudioMapper wordAudioMapper;
    private final TtsService ttsService;
    private final VoiceService voiceService;

    /** 为单词生成一条音频 */
    public WordAudio synthesize(String wordId, Voice voice, String text, String audioType) {
        String url = ttsService.synthesizeAndSave(voice, text,
                "word-" + audioType + "-" + wordId.substring(0, 8));

        WordAudio a = new WordAudio();
        a.setId(UUID.randomUUID().toString());
        a.setWordId(wordId);
        a.setVoiceId(voice.getId());
        a.setAudioType(audioType);
        a.setAudioUrl(url);
        wordAudioMapper.insert(a);
        return a;
    }

    /** 查已有音频（按 word_id 复用） */
    public WordAudio findExisting(String wordId, String voiceId, String audioType) {
        QueryWrapper<WordAudio> qw = new QueryWrapper<>();
        qw.eq("word_id", wordId).eq("voice_id", voiceId).eq("audio_type", audioType);
        return wordAudioMapper.selectOne(qw);
    }

    /** 获取或生成音频：先查已有，无则同步生成 */
    public WordAudio getOrGenerate(String wordId, String wordText, String exampleSentence, Voice voice, String audioType) {
        String text = "word".equals(audioType) ? wordText : exampleSentence;
        if (text == null || text.isBlank()) return null;

        WordAudio existing = findExisting(wordId, voice.getId(), audioType);
        if (existing != null) return existing;

        return synthesize(wordId, voice, text, audioType);
    }

    /** 用户生效音色 */
    public Voice getUserVoice(String voiceIdFromSettings) {
        return voiceService.getEffectiveVoice(voiceIdFromSettings);
    }
}
