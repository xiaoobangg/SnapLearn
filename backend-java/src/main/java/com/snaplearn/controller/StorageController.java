package com.snaplearn.controller;

import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @CurrentUser String userId) throws IOException {
        String url = storageService.uploadFile(userId, file);
        return Map.of("url", url);
    }
}
