package com.snaplearn.service.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * CosyVoice 声音复刻（REST 直调）。
 */
@Slf4j
@Service
public class VoiceEnrollmentService {

    @Value("${AI_DASHSCOPE_API_KEY}")
    private String apiKey;

    @Value("${DASHSCOPE_WORKSPACE_ID:}")
    private String workspaceId;

    @Value("${app.base-url:}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 文件上传模式 */
    public String enroll(MultipartFile file, String voiceName, String targetModel) throws Exception {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new RuntimeException("APP_BASE_URL 未配置");
        }
        Path dir = Path.of("uploads", "audio", "enroll");
        Files.createDirectories(dir);
        String fileName = "enroll-" + UUID.randomUUID().toString().substring(0, 8) + ".mp3";
        Path filePath = dir.resolve(fileName);
        long fileSize = file.getSize();
        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();
        log.info("[ENROLL] file received: name={} size={}bytes type={} voiceName={} targetModel={}",
                originalName, fileSize, contentType, voiceName, targetModel);
        try (var fos = new java.io.FileOutputStream(filePath.toFile())) {
            fos.write(file.getBytes());
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String audioUrl = base + "/uploads/audio/enroll/" + fileName;
        log.info("[ENROLL] file saved, public url={}", audioUrl);
        return enroll(audioUrl, voiceName, targetModel);
    }

    /** URL 模式 */
    public String enroll(String audioUrl, String voiceName, String targetModel) throws Exception {
        String safe = voiceName.replaceAll("[^a-zA-Z0-9]", "");
        if (safe.length() < 2) safe = "vo";
        if (safe.length() > 4) safe = safe.substring(0, 4);
        String prefix = safe + UUID.randomUUID().toString().substring(0, 6);

        String body = "{"
                + "\"model\":\"voice-enrollment\","
                + "\"input\":{"
                + "\"action\":\"create_voice\","
                + "\"target_model\":\"" + (targetModel != null ? targetModel : "cosyvoice-v3-flash") + "\","
                + "\"prefix\":\"" + prefix + "\","
                + "\"url\":\"" + escapeJson(audioUrl) + "\""
                + "}}";

        return call(body).path("output").path("voice_id").asText();
    }

    /** 查询已复刻音色列表 */
    public String listEnrolled() throws Exception {
        String body = "{\"model\":\"voice-enrollment\",\"input\":{\"action\":\"list_voice\",\"prefix\":\"\"}}";
        return call(body).toString();
    }

    /** 删除已复刻音色 */
    public void delete(String voiceId) throws Exception {
        String body = "{\"model\":\"voice-enrollment\",\"input\":{\"action\":\"delete_voice\",\"voice_id\":\"" + voiceId + "\"}}";
        call(body);
        log.info("[ENROLL] deleted voiceId={}", voiceId);
    }

    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/customization";

    private JsonNode call(String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(BASE_URL).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("X-DashScope-WorkSpace", workspaceId);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        int s = conn.getResponseCode();
        String resp = new String((s == 200 || s == 201 ? conn.getInputStream() : conn.getErrorStream()).readAllBytes(), StandardCharsets.UTF_8);
        if (s != 200 && s != 201) throw new RuntimeException("API error " + s + ": " + resp);
        return objectMapper.readTree(resp);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
