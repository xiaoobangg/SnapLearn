package com.snaplearn.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KnowledgePointResponse(
        String id,
        String pointType,
        String content,
        String status,
        Integer sortOrder
) {
}
