package com.snaplearn.service;

import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String uploadFile(String userId, MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        if (ext.isEmpty()) ext = ".jpg";

        String filename = userId + "_" + IdUtil.fastSimpleUUID() + ext;
        Path dir = Paths.get(uploadDir, "avatars").toAbsolutePath();
        Files.createDirectories(dir);
        Path filepath = dir.resolve(filename);
        // 必须用绝对路径，否则 Tomcat ApplicationPart 会解析到临时目录
        file.transferTo(filepath.toAbsolutePath().toFile());

        return "/uploads/avatars/" + filename;
    }
}
