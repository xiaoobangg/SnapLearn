package com.snaplearn.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DevLoginRequest(@NotBlank String phone) {
}
