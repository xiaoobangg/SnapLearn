package com.snaplearn.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardGroupResponse(
        String id,
        String title,
        String sourceImage,
        String sourceText,
        String createdAt,
        String groupStatus,
        List<CardResponse> cards
) {
    public CardGroupResponse(String id, String title, String sourceImage,
                             String sourceText, String createdAt,
                             List<CardResponse> cards) {
        this(id, title, sourceImage, sourceText, createdAt, null, cards);
    }
}