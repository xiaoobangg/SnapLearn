package com.snaplearn.controller;

import com.snaplearn.entity.Voice;
import com.snaplearn.service.UserSettingsService;
import com.snaplearn.service.VoiceService;
import com.snaplearn.service.tts.TtsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Coze 插件专用 TTS 接口。认证走 X-API-Key Header（ApiKeyAuthFilter）。
 */
@RestController
@RequestMapping("/api/v1/coze")
@RequiredArgsConstructor
public class CozeTtsController {

    private final TtsService ttsService;
    private final VoiceService voiceService;
    private final UserSettingsService userSettingsService;

    @Value("${app.base-url:}")
    private String baseUrl;

    @PostMapping("/tts")
    public Map<String, Object> tts(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String text = body.getOrDefault("text", "").toString();
        String voiceCode = body.getOrDefault("voice", "").toString();
        String userId = (String) request.getAttribute("userId");

        Voice voice;
        if (!voiceCode.isBlank()) {
            voice = voiceService.listActive().stream()
                    .filter(v -> v.getVoiceCode().equals(voiceCode))
                    .findFirst()
                    .orElse(null);
        } else {
            String prefVoiceId = userSettingsService.getVoiceId(userId);
            voice = voiceService.getEffectiveVoice(prefVoiceId);
        }
        if (voice == null) {
            return Map.of("error", "没有可用的音色，请先导入音色");
        }

        // 提取可选覆盖参数：接口传参优先于音色默认值
        Map<String, Object> overrides = new HashMap<>();
        if (body.containsKey("format")) overrides.put("format", body.get("format"));
        if (body.containsKey("sample_rate")) overrides.put("sample_rate", body.get("sample_rate"));
        if (body.containsKey("volume")) overrides.put("volume", body.get("volume"));
        if (body.containsKey("rate")) overrides.put("rate", body.get("rate"));
        if (body.containsKey("pitch")) overrides.put("pitch", body.get("pitch"));

        String relativeUrl = ttsService.synthesizeAndSave(voice, text, "coze-" + userId.substring(0, Math.min(8, userId.length())), overrides);
        String fullUrl = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.replaceAll("/$", "") + "/" + relativeUrl
                : relativeUrl;
        return Map.of("audio_url", fullUrl, "voice_code", voice.getVoiceCode());
    }
}
