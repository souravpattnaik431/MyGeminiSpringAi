package org.ai.gemini.geminiai.dto;

import module java.base;

/**
 * Request DTO for retrieving tool changes information.
 * <p>
 * This record encapsulates the list of tools for which the latest changes
 * should be fetched from the AI service.
 * </p>
 *
 * @param tools List of tool names to query (e.g., "azure-devops", "playwright",
 *              "selenium-java")
 * @author Gemini AI Service
 * @version 1.0
 * @since 2026-01-01
 */
public record ToolChangesRequest(List<String> tools) {
}
