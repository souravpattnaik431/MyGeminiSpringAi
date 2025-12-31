package org.ai.gemini.geminiai.dto;

/**
 * Result DTO representing the outcome of a single tool query.
 * <p>
 * This record contains the AI-generated content along with metadata about
 * the processing, including token usage and execution time.
 * </p>
 *
 * @param toolName         Name of the tool that was queried
 * @param content          Markdown-formatted content with the latest tool
 *                         updates
 * @param inputTokens      Number of tokens used in the AI prompt (null if error
 *                         occurred)
 * @param outputTokens     Number of tokens generated in the AI response (null
 *                         if error occurred)
 * @param processingTimeMs Time taken to process this tool query in milliseconds
 * @param status           Status of the query: "SUCCESS", "TIMEOUT", or "ERROR"
 * @author Gemini AI Service
 * @version 1.0
 * @since 2026-01-01
 */
public record ToolChangeResult(
        String toolName,
        String content,
        Integer inputTokens,
        Integer outputTokens,
        Long processingTimeMs,
        String status) {
}
