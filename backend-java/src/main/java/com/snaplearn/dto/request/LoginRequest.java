package com.snaplearn.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String code) {
}
