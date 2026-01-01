package org.ai.gemini.geminiai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesRequest;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.ai.gemini.geminiai.service.AiService;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
@Slf4j
public class AiController {

    private final AiService aiService;
    private final TemplateEngine templateEngine;

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
    @PostMapping(value = "/getAllToolChanges", produces = "text/html")
    public String getAllToolChanges(@RequestBody ToolChangesRequest request) {
        if (request.tools() == null || request.tools().isEmpty()) {
            throw new IllegalArgumentException("Tools list cannot be empty");
        }

        ToolChangesResponse response = aiService.getAllToolChanges(request.tools());

        Context context = new Context();
        context.setVariable("results", response.results());
        context.setVariable("totalProcessingTimeMs", response.totalProcessingTimeMs());
        context.setVariable("timestamp", java.time.LocalDateTime.now());

        String htmlContent = templateEngine.process("tool-updates", context);

        try {
            Path path = Paths.get("latest_tool_updates.html");
            Files.writeString(path, htmlContent);
            log.info("Saved report to: {}", path.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to save report to file", e);
        }

        return htmlContent;
    }
}
