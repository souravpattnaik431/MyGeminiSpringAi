package org.ai.gemini.geminiai.controller;

import lombok.RequiredArgsConstructor;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesRequest;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.ai.gemini.geminiai.service.AiService;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing AI-powered tool change queries.
 * <p>
 * This controller provides endpoints to fetch the latest updates and changes
 * for various development tools using AI-generated summaries.
 * </p>
 *
 * @author Gemini AI Service
 * @version 1.0
 * @since 2026-01-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini")
public class AiController {

    private final AiService aiService;

    /**
     * Retrieves the latest changes for a single tool.
     *
     * @param toolName Name of the tool to query (e.g., "playwright",
     *                 "selenium-java")
     * @return ToolChangeResult containing the AI-generated content and metadata
     */
    @GetMapping("/{toolName}")
    public ToolChangeResult getToolChanges(@PathVariable String toolName) {
        return aiService.getToolChangesWithMetadata(toolName);
    }

    /**
     * Retrieves the latest changes for multiple tools in parallel.
     * <p>
     * This endpoint processes multiple tool queries concurrently and returns
     * aggregated results with total token usage and processing time.
     * </p>
     *
     * @param request ToolChangesRequest containing the list of tools to query
     * @return ToolChangesResponse with individual results and aggregated statistics
     * @throws IllegalArgumentException if the tools list is null or empty
     */
    @PostMapping("/getAllToolChanges")
    public ToolChangesResponse getAllToolChanges(@RequestBody ToolChangesRequest request) {
        if (request.tools() == null || request.tools().isEmpty()) {
            throw new IllegalArgumentException("Tools list cannot be empty");
        }
        return aiService.getAllToolChanges(request.tools());
    }
}
