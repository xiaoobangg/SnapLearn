package com.snaplearn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduTTSService {

    private final BaiduOCRService baiduOCRService;
    private final RestTemplate restTemplate;

    public byte[] synthesize(String text, String lang) {
        // 先尝试百度 TTS
        // byte[] result = baiduTts(text, lang);
        byte[] result = youdaoTts(text, lang);
        if (result != null && result.length > 0) {
            return result;
        }
        // 降级：中文用百度翻译 TTS，英文用有道词典 TTS
        if (lang.equals("zh")) {
            log.info("百度 TTS 失败，降级到百度翻译 TTS");
            return baiduTranslateTts(text);
        } else {
            log.info("百度 TTS 失败，降级到有道词典 TTS");
            return youdaoTts(text, lang);
        }
    }

    private byte[] baiduTts(String text, String lang) {
        try {
            String accessToken = baiduOCRService.getAccessToken();
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String lan = lang.equals("zh") ? "zh" : "en";
            int per = lang.equals("zh") ? 4 : 0;

            String url = "http://tsn.baidu.com/text2audio";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = String.format(
                    "tex=%s&tok=%s&cuid=snaplearn_tts&ctp=1&lan=%s&spd=4&pit=5&vol=6&per=%d&aue=3",
                    encodedText, accessToken, lan, per
            );
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
            if (resp.getBody() == null || resp.getBody().length == 0) {
                log.warn("百度 TTS 返回空响应");
                return null;
            }

            // 百度 TTS 错误时返回 JSON
            if (resp.getBody()[0] == '{') {
                String errBody = new String(resp.getBody(), StandardCharsets.UTF_8);
                log.warn("百度 TTS 返回错误 JSON: {}", errBody);
                return null;
            }

            log.info("百度 TTS 成功，{} 字节", resp.getBody().length);
            return resp.getBody();
        } catch (Exception e) {
            log.error("百度 TTS 请求异常: {}", e.getMessage());
            return null;
        }
    }

    private byte[] baiduTranslateTts(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://fanyi.baidu.com/gettts?lan=zh&text=" + encoded + "&spd=3&source=web";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            if (resp.getBody() == null || resp.getBody().length == 0) {
                log.warn("百度翻译 TTS 返回空响应");
                return null;
            }
            if (resp.getBody()[0] == '{' || resp.getBody()[0] == '<') {
                log.warn("百度翻译 TTS 返回非音频内容，文本可能过长");
                return null;
            }

            log.info("百度翻译 TTS 成功，{} 字节", resp.getBody().length);
            return resp.getBody();
        } catch (Exception e) {
            log.error("百度翻译 TTS 请求异常: {}", e.getMessage());
            return null;
        }
    }

    private byte[] youdaoTts(String text, String lang) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            int type = lang.equals("zh") ? 0 : 1;
            String url = "https://dict.youdao.com/dictvoice?audio=" + encoded + "&type=" + type;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            if (resp.getBody() == null || resp.getBody().length == 0) {
                log.warn("有道 TTS 返回空响应");
                return null;
            }

            // 有道可能返回 HTML 错误页
            if (resp.getBody()[0] == '<') {
                log.warn("有道 TTS 返回 HTML 而非音频，可能文本过长");
                return null;
            }

            log.info("有道 TTS 成功，{} 字节", resp.getBody().length);
            return resp.getBody();
        } catch (Exception e) {
            log.error("有道 TTS 请求异常: {}", e.getMessage());
            return null;
        }
    }
}
