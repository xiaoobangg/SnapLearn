package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.CardMapper;
import com.snaplearn.mapper.WordContentMapper;
import com.snaplearn.mapper.WordMapper;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.CardAudioService;
import com.snaplearn.service.UserSettingsService;
import com.snaplearn.service.VoiceService;
import com.snaplearn.service.tts.TtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统一音频接口：GET /api/v1/tts?text=xxx&cardId=xxx&type=xxx
 * <p>
 * 前端始终传 text + cardId + type；后端按用户音色偏好匹配缓存或实时生成。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;
    private final VoiceService voiceService;
    private final CardAudioService cardAudioService;
    private final UserSettingsService userSettingsService;
    private final CardMapper cardMapper;
    private final WordMapper wordMapper;
    private final WordContentMapper wordContentMapper;

    @GetMapping("/tts")
    public Map<String, Object> tts(@RequestParam String text,
                                   @RequestParam(defaultValue = "") String cardId,
                                   @RequestParam(defaultValue = "") String type,
                                   @CurrentUser String userId) {
        // 用户偏好音色，未设置则取系统默认
        String prefVoiceId = userSettingsService.getVoiceId(userId);
        Voice voice = voiceService.getEffectiveVoice(prefVoiceId);

        // 有 cardId → 缓存优先
        if (!cardId.isBlank() && !type.isBlank()) {
            CardAudio cached = cardAudioService.findExisting(cardId, voice.getId(), type);
            if (cached != null) {
                return Map.of("audio_url", cached.getAudioUrl(), "cached", true);
            }

            CardAudio generated = generateForCard(cardId, type, voice, text);
            if (generated != null) {
                return Map.of("audio_url", generated.getAudioUrl(), "cached", false);
            }
        }

        // 纯文本
        String url = ttsService.synthesizeAndSave(voice, text,
                "tts-" + text.substring(0, Math.min(8, text.length())));
        return Map.of("audio_url", url);
    }

    private CardAudio generateForCard(String cardId, String type, Voice voice, String fallbackText) {
        try {
            Card card = cardMapper.selectById(cardId);
            if (card == null) return null;
            Word word = wordMapper.selectById(card.getWordId());
            WordContent wc = wordContentMapper.selectOne(
                    new QueryWrapper<WordContent>().eq("word_id", card.getWordId()));

            String realText = switch (type) {
                case "word" -> word != null ? word.getWordText() : fallbackText;
                case "general_meaning" -> wc != null ? wc.getGeneralMeaning() : fallbackText;
                case "extended_meaning" -> wc != null ? wc.getExtendedMeaning() : fallbackText;
                case "example_sentence",
                        "example" -> wc != null ? wc.getExampleSentence() : fallbackText;
                case "memory_tip" -> wc != null ? wc.getMemoryTip() : fallbackText;
                case "pronunciation" -> wc != null ? wc.getPronunciation() : fallbackText;
                case "pos" -> wc != null ? wc.getPos() : fallbackText;
                default -> fallbackText;
            };
            if (realText == null || realText.isBlank()) return null;
            return cardAudioService.synthesize(cardId, voice, realText, type);
        } catch (Exception e) {
            return null;
        }
    }
}
