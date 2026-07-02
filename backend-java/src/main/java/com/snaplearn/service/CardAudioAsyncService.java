package com.snaplearn.service;

import com.snaplearn.entity.Voice;
import com.snaplearn.entity.WordAudio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardAudioAsyncService {

    private final CardAudioService cardAudioService;

    @Async("ttsExecutor")
    public void preGenerate(List<Map<String, String>> items, Voice voice) {
        log.info("[AUDIO-ASYNC] preGenerate items={} voiceId={}", items.size(), voice.getId());
        for (Map<String, String> item : items) {
            doGenerate(item.get("wordId"), voice, item.get("text"), item.get("type"));
        }
        log.info("[AUDIO-ASYNC] preGenerate done items={}", items.size());
    }

    private void doGenerate(String wordId, Voice voice, String text, String audioType) {
        try {
            WordAudio existing = cardAudioService.findExisting(wordId, voice.getId(), audioType);
            if (existing != null) return;
            cardAudioService.synthesize(wordId, voice, text, audioType);
        } catch (Exception e) {
            log.warn("[AUDIO-ASYNC] gen failed wordId={} type={} text.len={}", wordId, audioType, text.length(), e);
        }
    }
}
