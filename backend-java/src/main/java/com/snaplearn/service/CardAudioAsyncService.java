package com.snaplearn.service;

import com.snaplearn.entity.CardAudio;
import com.snaplearn.entity.Voice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 异步预生成卡片音频。
 * <br>
 * 卡片组创建完成后调用，后台逐个生成，不阻塞创建流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardAudioAsyncService {

    private final CardAudioService cardAudioService;

    /**
     * 预生成多张卡片的语音（异步触发）。
     * @param items 每项包含 cardId / type / text，覆盖所有知识点类型
     * @param voice 使用的音色
     */
    @Async("ttsExecutor")
    public void preGenerate(List<Map<String, String>> items, Voice voice) {
        log.info("[AUDIO-ASYNC] preGenerate items={} voiceId={}", items.size(), voice.getId());
        for (Map<String, String> item : items) {
            doGenerate(item.get("cardId"), voice, item.get("text"), item.get("type"));
        }
        log.info("[AUDIO-ASYNC] preGenerate done items={}", items.size());
    }

    private void doGenerate(String cardId, Voice voice, String text, String audioType) {
        try {
            CardAudio existing = cardAudioService.findExisting(cardId, voice.getId(), audioType);
            if (existing != null) return;
            cardAudioService.synthesize(cardId, voice, text, audioType);
        } catch (Exception e) {
            log.warn("[AUDIO-ASYNC] gen failed cardId={} type={} text.len={}", cardId, audioType, text.length(), e);
        }
    }
}
