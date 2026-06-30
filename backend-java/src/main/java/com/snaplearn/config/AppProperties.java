package com.snaplearn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Wechat wechat = new Wechat();
    private LlmProperties llm = new LlmProperties();
    private BaiduOcr baiduOcr = new BaiduOcr();
    private Upload upload = new Upload();
    private AdminConfig admin = new AdminConfig();
    private boolean debug = true;

    @Data
    public static class Jwt {
        private String secret;
        private long expireMinutes = 10080;
    }

    @Data
    public static class Wechat {
        private String appId;
        private String appSecret;
    }

    @Data
    public static class LlmConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String model = "qwen-plus";
    }

    @Data
    public static class LlmProperties {
        private String defaultProvider = "qwen";
        private Map<String, LlmConfig> providers = new LinkedHashMap<>();
    }

    @Data
    public static class BaiduOcr {
        private String apiKey;
        private String secretKey;
    }

    @Data
    public static class Upload {
        private String dir = "uploads";
    }

    @Data
    public static class AdminConfig {
        private Jwt jwt = new Jwt();
    }

    public LlmConfig getLlmConfig(String provider) {
        LlmConfig config = llm.getProviders().get(provider);
        if (config == null) {
            throw new IllegalArgumentException("Unknown LLM provider: " + provider);
        }
        return config;
    }
}
