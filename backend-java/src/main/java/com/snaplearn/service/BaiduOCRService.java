package com.snaplearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.config.AppProperties;
import com.snaplearn.dto.response.OCRResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduOCRService {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate;
    private final WordService wordService;

    private volatile String cachedAccessToken;
    private volatile LocalDateTime tokenExpiresAt;

    public OCRResponse recognize(byte[] imageBytes) {
        String accessToken = getAccessToken();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String urlEncodedImage = URLEncoder.encode(base64Image, StandardCharsets.UTF_8);

        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic?access_token=" + accessToken;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> entity = new HttpEntity<>("image=" + urlEncodedImage, headers);

        try {
            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(url, entity, JsonNode.class);
            JsonNode body = resp.getBody();
            if (body == null) {
                throw new BusinessException(503, "百度 OCR 无响应");
            }
            if (body.has("error_code")) {
                String errMsg = body.path("error_msg").asText("unknown error");
                throw new BusinessException(503, "百度 OCR 错误: " + errMsg);
            }

            StringBuilder fullText = new StringBuilder();
            JsonNode wordsResult = body.get("words_result");
            if (wordsResult != null && wordsResult.isArray()) {
                for (JsonNode item : wordsResult) {
                    String w = item.path("words").asText("");
                    if (!w.isEmpty()) {
                        if (fullText.length() > 0) fullText.append(" ");
                        fullText.append(w);
                    }
                }
            }

            String text = fullText.toString();
            List<String> words = wordService.extractWords(text);
            return new OCRResponse(text, words);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度 OCR 请求失败", e);
            throw new BusinessException(503, "百度 OCR 服务异常");
        }
    }

    synchronized String getAccessToken() {
        if (cachedAccessToken != null && LocalDateTime.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }
        String apiKey = appProperties.getBaiduOcr().getApiKey();
        String secretKey = appProperties.getBaiduOcr().getSecretKey();
        String url = String.format(
                "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                apiKey, secretKey
        );
        try {
            JsonNode resp = restTemplate.getForObject(url, JsonNode.class);
            if (resp == null || resp.has("error")) {
                String err = resp != null ? resp.path("error_description").asText("") : "";
                throw new BusinessException(503, "百度 OCR 认证失败: " + err);
            }
            cachedAccessToken = resp.get("access_token").asText();
            long expiresIn = resp.path("expires_in").asLong(2592000);
            tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn - 60);
            return cachedAccessToken;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度 OCR token 获取失败", e);
            throw new BusinessException(503, "百度 OCR 服务不可用");
        }
    }
}
