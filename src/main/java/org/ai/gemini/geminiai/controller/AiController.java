package org.ai.gemini.geminiai.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesRequest;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.ai.gemini.geminiai.service.AiService;
import org.ai.gemini.geminiai.service.MarkdownReportService;

/**
 * REST Controller for managing AI-powered tool change queries.
 * <p>
 * This controller provides endpoints to fetch the latest updates and changes
 * for various development tools using AI-generated structured reports.
 * </p>
 *
 * @author Gemini AI Service
 * @version 2.0
 * @since 2026-01-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini")
@Slf4j
public class AiController {

    private final AiService aiService;
    private final MarkdownReportService markdownReportService;

    /**
     * Retrieves the latest structured changes for a single tool.
     *
     * @param toolName Name of the tool to query (e.g., "playwright", "selenium-java")
     * @return ToolChangeResult containing structured ToolReport and metadata
     */
    @GetMapping("/{toolName}")
    public ToolChangeResult getToolChanges(@PathVariable String toolName) {
        return aiService.getToolChangesWithMetadata(toolName);
    }

    /**
     * Retrieves the latest changes for multiple tools in parallel and generates a Markdown digest.
     *
     * @param request ToolChangesRequest containing the list of tools to query
     * @return Formatted Markdown report
     * @throws IllegalArgumentException if the tools list is null or empty
     */
    @PostMapping(value = "/getAllToolChanges", produces = "text/markdown")
    public String getAllToolChanges(@RequestBody ToolChangesRequest request) {
        if (request.tools() == null || request.tools().isEmpty()) {
            throw new IllegalArgumentException("Tools list cannot be empty");
        }

        ToolChangesResponse response = aiService.getAllToolChanges(request.tools());
        return markdownReportService.generateAndSaveReport(response);
    }
}
