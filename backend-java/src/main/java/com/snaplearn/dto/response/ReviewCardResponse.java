package com.snaplearn.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewCardResponse(
        String notebookId,
        String cardId,
        String word,
        String generalMeaning,
        String extendedMeaning,
        String exampleSentence,
        String memoryTip,
        String pronunciation,
        String pos
) {
}