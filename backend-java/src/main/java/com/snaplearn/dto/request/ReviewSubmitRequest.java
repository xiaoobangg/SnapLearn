package com.snaplearn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewSubmitRequest(
        @NotBlank String cardId,
        @Min(0) @Max(5) int quality
) {
}
