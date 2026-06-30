package com.snaplearn.dto.request;

public record MoveCardRequest(
        String targetGroupId,
        String newGroupTitle
) {
}
