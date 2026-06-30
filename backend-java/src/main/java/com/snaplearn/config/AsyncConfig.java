package com.snaplearn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * TTS 等异步任务专用线程池，避免占用 Spring 默认 SimpleAsyncTaskExecutor 无限制创建线程。
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "ttsExecutor")
    public Executor ttsExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(16);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("tts-");
        exec.setKeepAliveSeconds(60);
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.initialize();
        return exec;
    }
}
