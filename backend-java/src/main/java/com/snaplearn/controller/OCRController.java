package com.snaplearn.controller;

import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.dto.response.OCRResponse;
import com.snaplearn.service.BaiduOCRService;
import com.snaplearn.service.LLMService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
public class OCRController {

    private final BaiduOCRService baiduOCRService;
    private final LLMService llmService;

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]{2,}");

    @PostMapping("/recognize")
    public OCRResponse recognize(@RequestParam("image") MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            throw new BusinessException(400, "请上传图片");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "请上传图片文件");
        }
        return baiduOCRService.recognize(image.getBytes());
    }

    /**
     * OCR + AI：百度 OCR 识别后，将完整结果交给 AI 提取英文单词
     */
    @PostMapping("/recognize-ai")
    public Map<String, Object> recognizeWithAI(@RequestParam("image") MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            throw new BusinessException(400, "请上传图片");
        }
        // 1. 百度 OCR
        OCRResponse ocrResult = baiduOCRService.recognize(image.getBytes());
        String ocrText = ocrResult.text();
        // 2. OCR 结果发给 AI，让 AI 判断提取英文单词
        String prompt = "请从以下OCR识别结果中提取所有英文单词，只返回逗号分隔的单词列表，不要其他内容。如果没有英文单词则返回\"无\"：\n" + ocrText;
        String aiReply = llmService.chatSimple(prompt, "deepseek");
        // 3. 从 AI 回复中提取单词
        List<String> words = extractWords(aiReply);
        return Map.of("words", words);
    }

    private List<String> extractWords(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        Matcher m = WORD_PATTERN.matcher(text);
        while (m.find()) {
            String w = m.group().toLowerCase();
            if (!result.contains(w)) result.add(w);
        }
        return result;
    }
}
