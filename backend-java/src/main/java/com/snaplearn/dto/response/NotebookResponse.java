package com.snaplearn.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotebookResponse(
        String id,
        String cardId,
        String word,
        String generalMeaning,
        String pos,
        String status,
        Double easeFactor,
        Integer intervalDays,
        String nextReviewAt,
        String lastReviewAt
) {
}