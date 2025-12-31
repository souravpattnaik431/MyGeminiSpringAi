package org.ai.gemini.geminiai.dto;

public record ToolChangeResult(
        String toolName,
        String content,
        Integer inputTokens,
        Integer outputTokens,
        Long processingTimeMs,
        String status) {
}
