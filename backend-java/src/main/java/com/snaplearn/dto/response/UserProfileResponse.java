package com.snaplearn.dto.response;

public record UserProfileResponse(
        String id,
        String phone,
        String nickname,
        String avatarUrl,
        String createdAt
) {
}