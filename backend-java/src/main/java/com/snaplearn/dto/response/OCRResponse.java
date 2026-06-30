package com.snaplearn.dto.response;

import java.util.List;

public record OCRResponse(String text, List<String> words) {
}