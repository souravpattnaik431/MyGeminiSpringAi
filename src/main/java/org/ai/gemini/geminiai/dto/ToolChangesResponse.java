package org.ai.gemini.geminiai.dto;

import module java.base;

public record ToolChangesResponse(
        List<ToolChangeResult> results,
        Integer totalTools,
        Long totalProcessingTimeMs,
        Integer totalInputTokens,
        Integer totalOutputTokens,
        String timestamp) {
}
