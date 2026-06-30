package com.snaplearn.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 从 classpath:/prompts/ 目录加载 AI 提示词模板
 */
@Component
public class PromptLoader {

    public String load(String name) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + name);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + name, e);
        }
    }
}
