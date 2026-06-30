package com.snaplearn.dto.response;

public record AuthResponse(
        String userId,
        String phone,
        String nickname,
        boolean isNew,
        String token
) {
}