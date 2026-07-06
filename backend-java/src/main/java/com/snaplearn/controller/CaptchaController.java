package com.snaplearn.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1")
public class CaptchaController {

    private static final ConcurrentHashMap<String, Captcha> STORE = new ConcurrentHashMap<>();

    private record Captcha(String code, long expiresAt) {}

    @GetMapping("/captcha")
    public Map<String, String> captcha() {
        // 清理过期
        long now = System.currentTimeMillis();
        STORE.entrySet().removeIf(e -> e.getValue().expiresAt < now);

        String code = randomCode(4);
        String key = UUID.randomUUID().toString().substring(0, 8);
        STORE.put(key, new Captcha(code, now + 300_000)); // 5 分钟有效

        String base64 = generateImage(code);
        return Map.of("key", key, "image", "data:image/png;base64," + base64);
    }

    /** 校验验证码，验证后立即删除 */
    public static boolean verify(String key, String code) {
        if (key == null || code == null) return false;
        Captcha c = STORE.remove(key);
        return c != null && c.code.equalsIgnoreCase(code) && c.expiresAt > System.currentTimeMillis();
    }

    private String randomCode(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateImage(String code) {
        int w = 120, h = 44;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 背景
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, w, h);

        // 干扰线
        g.setColor(new Color(200, 210, 220));
        for (int i = 0; i < 4; i++) {
            int x1 = ThreadLocalRandom.current().nextInt(w);
            int y1 = ThreadLocalRandom.current().nextInt(h);
            g.drawLine(x1, y1, x1 + 20, y1 - 10 + ThreadLocalRandom.current().nextInt(20));
        }

        // 文字
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(60 + ThreadLocalRandom.current().nextInt(80),
                    60 + ThreadLocalRandom.current().nextInt(80),
                    120 + ThreadLocalRandom.current().nextInt(80)));
            g.drawString(String.valueOf(code.charAt(i)), 10 + i * 26,
                    30 + ThreadLocalRandom.current().nextInt(8));
        }
        g.dispose();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成验证码失败", e);
        }
    }
}
