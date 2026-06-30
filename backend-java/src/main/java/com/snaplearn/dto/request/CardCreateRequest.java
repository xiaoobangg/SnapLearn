package com.snaplearn.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CardCreateRequest(
        String sourceImage,
        String sourceText,
        @NotEmpty List<String> selectedWords
) {
}
