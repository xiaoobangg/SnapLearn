package com.snaplearn.service.tts;

import com.snaplearn.entity.Voice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TTS 调度：根据 Voice.provider 路由到对应 Provider，合成后写本地文件，返回相对 URL。
 */
@Slf4j
@Service
public class TtsService {

    private final List<TtsProvider> providers;
    private final Path audioDir;

    public TtsService(List<TtsProvider> providers, @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.providers = providers;
        this.audioDir = Path.of(uploadDir, "audio").toAbsolutePath();
    }

    /**
     * 合成一条语音，保存为文件，返回相对 URL。
     *
     * @param voice 音色对象（含 model/format/sampleRate/volume/speechRate/pitch/instruction）
     */
    public String synthesizeAndSave(Voice voice, String text, String fileNamePrefix) {
        return synthesizeAndSave(voice, text, fileNamePrefix, null);
    }

    /**
     * 合成一条语音，支持通过 overrides 覆盖音色默认参数。
     *
     * @param voice    音色对象（默认参数来源）
     * @param overrides 可选覆盖，key: format / sample_rate / volume / rate / pitch，value 为对应类型
     */
    public String synthesizeAndSave(Voice voice, String text, String fileNamePrefix, Map<String, Object> overrides) {
        TtsProvider p = providers.stream()
                .filter(x -> x.code().equals(voice.getProvider()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Provider not found: " + voice.getProvider()));
        try {
            String fmt = getStringOverride(overrides, "format", voice.getFormat(), "mp3");
            int sr = getIntOverride(overrides, "sample_rate", voice.getSampleRate(), 22050);
            int vol = getIntOverride(overrides, "volume", voice.getVolume(), 50);
            double rate = getDoubleOverride(overrides, "rate", voice.getSpeechRate(), 1.0);
            double pitch = getDoubleOverride(overrides, "pitch", voice.getPitch(), 1.0);
            byte[] audio = p.synthesize(text, voice.getVoiceCode(), voice.getTtsModel(), fmt, sr, vol, rate, pitch, voice.getInstruction());
            Files.createDirectories(audioDir);
            String ext = fmt != null && !fmt.isBlank() ? fmt : "mp3";
            String fileName = fileNamePrefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path filePath = audioDir.resolve(fileName);
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(audio);
            }
            log.info("[TTS] saved {} provider={} voice={} size={}",
                    fileName, voice.getProvider(), voice.getVoiceCode(), audio.length);
            return "uploads/audio/" + fileName;
        } catch (Exception e) {
            log.error("[TTS] synth failed provider={} voice={} text.len={}", voice.getProvider(), voice.getVoiceCode(), text.length(), e);
            throw new RuntimeException("TTS failed: " + e.getMessage(), e);
        }
    }

    private static String getStringOverride(Map<String, Object> overrides, String key, String voiceVal, String defaultVal) {
        if (overrides != null && overrides.get(key) != null) return overrides.get(key).toString();
        return voiceVal != null ? voiceVal : defaultVal;
    }

    private static int getIntOverride(Map<String, Object> overrides, String key, Integer voiceVal, int defaultVal) {
        if (overrides != null && overrides.get(key) instanceof Number n) return n.intValue();
        return voiceVal != null ? voiceVal : defaultVal;
    }

    private static double getDoubleOverride(Map<String, Object> overrides, String key, Double voiceVal, double defaultVal) {
        if (overrides != null && overrides.get(key) instanceof Number n) return n.doubleValue();
        return voiceVal != null ? voiceVal : defaultVal;
    }
}
