package com.snaplearn.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardResponse(
        String id,
        String word,
        String generalMeaning,
        String extendedMeaning,
        String exampleSentence,
        String memoryTip,
        String pos,
        String pronunciation,
        String createdAt,
        String cardStatus,
        String wordId,
        List<KnowledgePointResponse> knowledgePoints
) {
    public CardResponse(String id, String word, String generalMeaning, String extendedMeaning,
                        String exampleSentence, String memoryTip, String pos,
                        String pronunciation, String createdAt) {
        this(id, word, generalMeaning, extendedMeaning, exampleSentence, memoryTip,
                pos, pronunciation, createdAt, null, null, null);
    }
}