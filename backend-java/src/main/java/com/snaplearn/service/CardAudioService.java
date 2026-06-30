package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.dto.response.CardResponse;
import com.snaplearn.entity.CardAudio;
import com.snaplearn.entity.Voice;
import com.snaplearn.entity.WordContent;
import com.snaplearn.mapper.CardAudioMapper;
import com.snaplearn.service.tts.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 卡片音频服务。
 * <br>异步生成入口：{@link CardAudioAsyncService}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardAudioService {

    private final CardAudioMapper cardAudioMapper;
    private final TtsService ttsService;
    private final VoiceService voiceService;

    /** 为单卡生成一条音频（同步，仅用于按需生成）。 */
    public CardAudio synthesize(String cardId, Voice voice, String text, String audioType) {
        String url = ttsService.synthesizeAndSave(voice, text,
                "card-" + audioType + "-" + cardId.substring(0, 8));

        CardAudio a = new CardAudio();
        a.setId(UUID.randomUUID().toString());
        a.setCardId(cardId);
        a.setVoiceId(voice.getId());
        a.setAudioType(audioType);
        a.setAudioUrl(url);
        cardAudioMapper.insert(a);
        return a;
    }

    /** 查已有音频 */
    public CardAudio findExisting(String cardId, String voiceId, String audioType) {
        QueryWrapper<CardAudio> qw = new QueryWrapper<>();
        qw.eq("card_id", cardId).eq("voice_id", voiceId).eq("audio_type", audioType);
        return cardAudioMapper.selectOne(qw);
    }

    /** 获取或生成音频：先查已有音频是否匹配当前音色，无则同步生成。 */
    public CardAudio getOrGenerate(String cardId, WordContent wc, CardResponse cr, Voice voice, String audioType) {
        String text = "word".equals(audioType) ? cr.word() : wc.getExampleSentence();
        if (text == null || text.isBlank()) return null;

        CardAudio existing = findExisting(cardId, voice.getId(), audioType);
        if (existing != null) return existing;

        return synthesize(cardId, voice, text, audioType);
    }

    /** 用户生效音色 */
    public Voice getUserVoice(String voiceIdFromSettings) {
        return voiceService.getEffectiveVoice(voiceIdFromSettings);
    }
}
