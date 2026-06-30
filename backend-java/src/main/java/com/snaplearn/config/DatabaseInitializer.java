package com.snaplearn.config;

import com.snaplearn.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final AdminService adminService;

    @Override
    public void run(String... args) {
        // Flyway 已接管 schema 迁移，不再手动执行 schema.sql
        try {
            adminService.ensureAdminExists();
            log.info("Default admin account ensured");
        } catch (Exception e) {
            log.error("Failed to ensure admin account", e);
        }
    }
}
