package com.snaplearn.service.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Coze 工作流 TTS 语音合成。
 * <p>
 * 调用 Coze 工作流 API，传入文本，工作流内部处理语音合成并返回音频。
 * 配置通过 Voice 表字段映射：
 * <ul>
 *   <li>voice_code → Coze Workflow ID</li>
 *   <li>tts_model → Coze Space ID</li>
 * </ul>
 * API Token 从环境变量 COZE_API_TOKEN 读取，不落数据库。
 */
@Slf4j
@Component
public class CozeWorkflowTtsProvider implements TtsProvider {

    @Value("${app.coze.api.token}")
    private String apiToken;

    @Override
    public String code() {
        return "coze";
    }

    @Override
    public byte[] synthesize(String text, String voiceCode, String model, String format,
                              int sampleRate, int volume, double speechRate, double pitch,
                              String instruction) throws Exception {
        // voiceCode = workflow_id, model = space_id
        String workflowId = voiceCode;
        String spaceId = (model != null && !model.isBlank()) ? model : "";

        String requestBody = "{"
                + "\"workflow_id\":\"" + workflowId + "\","
                + "\"parameters\":{"
                + "\"input\":\"" + escapeJson(text) + "\""
                + "}";
        if (!spaceId.isEmpty()) {
            requestBody += ",\"space_id\":\"" + escapeJson(spaceId) + "\"";
        }
        requestBody += "}";

        log.info("[TTS-COZE] workflow_id={} space_id={} text_len={}", workflowId, spaceId, text.length());

        // Step 1: Trigger workflow run
        HttpURLConnection conn = (HttpURLConnection) URI.create("https://api.coze.cn/v1/workflow/run")
                .toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);

        conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        if (status != 200) {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Coze TTS error " + status + ": " + err);
        }

        // Parse response: Coze workflow API returns
        // {"code":0,"data":"{\"output\":\"https://...mp3?...\"}"}
        String respJson = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.info("[TTS-COZE] Response length={}", respJson.length());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(respJson);

        // Check Coze API status
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String msg = root.path("msg").asText("");
            throw new RuntimeException("Coze workflow error code=" + code + ": " + msg);
        }

        // The "data" field is an escaped JSON string → parse it again
        String dataStr = root.path("data").asText();
        if (dataStr.isEmpty()) {
            throw new RuntimeException("Coze workflow response has empty data field. Response: "
                    + respJson.substring(0, Math.min(respJson.length(), 500)));
        }

        JsonNode dataNode = mapper.readTree(dataStr);
        // Try "output" first, then "audio_url"
        String audioUrl = dataNode.path("output").asText();
        if (audioUrl.isEmpty()) {
            audioUrl = dataNode.path("audio_url").asText();
        }
        if (audioUrl.isEmpty()) {
            throw new RuntimeException("Coze workflow data has no 'output' or 'audio_url' field. data: "
                    + dataStr.substring(0, Math.min(dataStr.length(), 500)));
        }

        // Download audio from URL
        log.info("[TTS-COZE] Downloading audio from: {}", audioUrl);
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
