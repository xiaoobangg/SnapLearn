package com.snaplearn.service.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 阿里云 DashScope CosyVoice 非实时语音合成。
 * <p>
 * API: POST /api/v1/services/audio/tts/SpeechSynthesizer
 * 模型: cosyvoice-v3-flash（延迟要求不高，便宜，支持丰富音色）
 * 地域: 北京（cn-beijing.maas.aliyuncs.com）
 * <p>
 * 文档: https://help.aliyun.com/zh/model-studio/cosyvoice
 */
@Slf4j
@Component
public class DashScopeCosyVoiceProvider implements TtsProvider {

    @Value("${AI_DASHSCOPE_API_KEY}")
    private String apiKey;

    /**
     * 百炼业务空间 ID，在控制台 https://bailian.console.aliyun.com 获取。
     * 如未在控制台创建业务空间，直接使用默认 API 端点。
     */
    @Value("${DASHSCOPE_WORKSPACE_ID:}")
    private String workspaceId;

    @Value("${DASHSCOPE_TTS_ENDPOINT:}")
    private String customEndpoint;

    @Override
    public String code() {
        return "dashscope";
    }

    @Override
    public byte[] synthesize(String text, String voiceCode, String model, String format, int sampleRate, int volume, double speechRate, double pitch, String instruction) throws Exception {
        String m = model != null && !model.isBlank() ? model : "cosyvoice-v3-plus";
        String fmt = format != null && !format.isBlank() ? format : "mp3";
        StringBuilder body = new StringBuilder("{"
                + "\"model\":\"" + m + "\","
                + "\"input\":{"
                + "\"text\":\"" + escapeJson(text) + "\","
                + "\"voice\":\"" + voiceCode + "\","
                + "\"format\":\"" + fmt + "\","
                + "\"sample_rate\":" + sampleRate + ","
                + "\"volume\":" + volume + ","
                + "\"rate\":" + speechRate + ","
                + "\"pitch\":" + pitch);
        if (instruction != null && !instruction.isBlank()) {
            body.append(",\"instruction\":\"" + escapeJson(instruction) + "\"");
        }
        body.append("}}");
        log.debug("[TTS-DASHSCOPE] model={} voice={} fmt={} sr={} vol={} rate={} pitch={}", m, voiceCode, fmt, sampleRate, volume, speechRate, pitch);

        String url;
        if (customEndpoint != null && !customEndpoint.isBlank()) {
            url = customEndpoint;
        } else {
            url = "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer";
        }

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("X-DashScope-WorkSpace", workspaceId);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        String bodyStr = body.toString();
        log.info("[TTS-DASHSCOPE] url={} workspace={} body={}",
                url, workspaceId, bodyStr);
        conn.getOutputStream().write(bodyStr.getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        if (status != 200) {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("DashScope TTS error " + status + ": " + err);
        }

        // 响应 JSON 含 output.audio.url（有效期 24h）
        // 格式: {"output":{"audio":{"url":"https://...","expires_at":"..."}}}
        String respJson = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String marker = "\"url\":\"";
        int start = respJson.indexOf(marker);
        if (start < 0) {
            throw new RuntimeException("Unexpected DashScope TTS response: " + respJson);
        }
        int end = respJson.indexOf("\"", start + marker.length());
        String audioUrl = respJson.substring(start + marker.length(), end);

        // 下载音频到内存
        HttpURLConnection dl = (HttpURLConnection) URI.create(audioUrl).toURL().openConnection();
        dl.setConnectTimeout(10000);
        dl.setReadTimeout(15000);
        try (InputStream is = dl.getInputStream()) {
            return is.readAllBytes();
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
