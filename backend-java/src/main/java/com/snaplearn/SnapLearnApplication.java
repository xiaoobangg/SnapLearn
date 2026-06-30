package com.snaplearn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.snaplearn.mapper")
public class SnapLearnApplication {
    public static void main(String[] args) {
        SpringApplication.run(SnapLearnApplication.class, args);
    }
}
