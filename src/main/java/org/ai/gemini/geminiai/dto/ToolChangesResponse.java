package org.ai.gemini.geminiai.dto;

import module java.base;

/**
 * Response DTO containing aggregated results from multiple tool queries.
 * <p>
 * This record encapsulates all individual tool results along with
 * aggregated statistics including total token usage and processing time.
 * </p>
 *
 * @param results               List of individual tool query results
 * @param totalTools            Total number of tools processed
 * @param totalProcessingTimeMs Total time taken to process all tools in
 *                              milliseconds
 * @param totalInputTokens      Sum of all input tokens used across all tools
 * @param totalOutputTokens     Sum of all output tokens generated across all
 *                              tools
 * @param timestamp             ISO timestamp when the response was generated
 * @author Gemini AI Service
 * @version 1.0
 * @since 2026-01-01
 */
public record ToolChangesResponse(
        List<ToolChangeResult> results,
        Integer totalTools,
        Long totalProcessingTimeMs,
        Integer totalInputTokens,
        Integer totalOutputTokens,
        String timestamp) {
}
