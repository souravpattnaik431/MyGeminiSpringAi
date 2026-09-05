package org.ai.gemini.geminiai.service;

import module java.base;
import lombok.extern.slf4j.Slf4j;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.ai.gemini.geminiai.dto.ToolReport;
import org.ai.gemini.geminiai.dto.ToolUpdateItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service to generate and save clean, formatted Markdown reports from structured AI responses.
 *
 * @author Gemini AI Service
 * @version 2.0
 * @since 2026-01-01
 */
@Service
@Slf4j
public class MarkdownReportService {

    private static final String DEFAULT_OUTPUT_FILE = "latest_tool_updates.md";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates a clean Markdown report from a {@link ToolChangesResponse} and saves it to disk.
     *
     * @param response Aggregated response containing individual tool change results.
     * @return Formatted Markdown string.
     */
    public String generateAndSaveReport(ToolChangesResponse response) {
        StringBuilder md = new StringBuilder();
        appendHeader(md, response);

        if (response.results() == null || response.results().isEmpty()) {
            md.append("_No tool results available._\n");
        } else {
            response.results().forEach(result -> appendToolResult(md, result));
        }

        String markdownContent = md.toString();
        saveToFile(markdownContent);
        return markdownContent;
    }

    /**
     * Appends the high-level report header, generation timestamp, and aggregated metrics to the report.
     *
     * @param md       The {@link StringBuilder} accumulating the Markdown content.
     * @param response The aggregated response containing metrics and tool results.
     */
    private void appendHeader(StringBuilder md, ToolChangesResponse response) {
        String formattedTime = LocalDateTime.now(ZoneId.systemDefault()).format(DATE_FORMATTER);

        md.append("# 🚀 Technical Tools Update Digest\n\n");
        md.append("> **Generated**: `").append(formattedTime).append("`  \n");
        md.append("> **Total Tools Processed**: `").append(response.totalTools()).append("` | ");
        md.append("**Total Processing Time**: `").append(response.totalProcessingTimeMs()).append(" ms`");

        if (response.totalInputTokens() != null && response.totalOutputTokens() != null) {
            md.append(" | **Total Tokens (In/Out)**: `")
              .append(response.totalInputTokens()).append(" / ").append(response.totalOutputTokens()).append("`");
        }
        md.append("\n\n---\n\n");
    }

    /**
     * Appends the section for an individual tool, including its metadata, structured report or error message.
     *
     * @param md     The {@link StringBuilder} accumulating the Markdown content.
     * @param result The result object for a specific tool query.
     */
    private void appendToolResult(StringBuilder md, ToolChangeResult result) {
        md.append("## 📦 ").append(result.toolName().toUpperCase()).append("\n\n");
        appendToolMetadata(md, result);

        if (result.report() != null) {
            appendToolReport(md, result.report());
        } else {
            appendToolError(md, result);
        }

        md.append("---\n\n");
    }

    /**
     * Appends metadata for a single tool query (status, processing time, input/output token counts).
     *
     * @param md     The {@link StringBuilder} accumulating the Markdown content.
     * @param result The tool result containing query metadata.
     */
    private void appendToolMetadata(StringBuilder md, ToolChangeResult result) {
        md.append("**Status**: `").append(result.status()).append("`");
        if (result.processingTimeMs() != null) {
            md.append(" | **Time**: `").append(result.processingTimeMs()).append(" ms`");
        }
        if (result.inputTokens() != null && result.outputTokens() != null) {
            md.append(" | **Tokens**: `").append(result.inputTokens()).append(" in / ").append(result.outputTokens()).append(" out`");
        }
        md.append("\n\n");
    }

    /**
     * Appends the structured report content, including the high-level overview and update item list.
     *
     * @param md     The {@link StringBuilder} accumulating the Markdown content.
     * @param report The structured tool report containing overview and update items.
     */
    private void appendToolReport(StringBuilder md, ToolReport report) {
        if (report.overview() != null && !report.overview().isBlank()) {
            md.append("> ").append(report.overview()).append("\n\n");
        }

        if (report.updates() == null || report.updates().isEmpty()) {
            md.append("_No specific updates reported for this timeframe._\n\n");
            return;
        }

        report.updates().forEach(item -> appendUpdateItem(md, item));
    }

    /**
     * Appends a single update item with its title, impact badge, date, source, summary, and action items.
     *
     * @param md   The {@link StringBuilder} accumulating the Markdown content.
     * @param item The update item containing specific change details.
     */
    private void appendUpdateItem(StringBuilder md, ToolUpdateItem item) {
        String impactEmoji = getImpactEmoji(item.impactLevel());
        String title = (item.title() != null && !item.title().isBlank()) ? item.title() : "Update";

        md.append("### ").append(impactEmoji).append(" ").append(title).append("\n\n");
        appendItemDateAndSource(md, item);

        if (item.summary() != null && !item.summary().isBlank()) {
            md.append("- **Summary**: ").append(item.summary()).append("\n");
        }

        appendItemActionItems(md, item.actionItems());
        md.append("\n");
    }

    /**
     * Formats and appends the date and source information for an update item if present.
     *
     * @param md   The {@link StringBuilder} accumulating the Markdown content.
     * @param item The update item containing date and source fields.
     */
    private void appendItemDateAndSource(StringBuilder md, ToolUpdateItem item) {
        boolean hasDate = item.date() != null && !item.date().isBlank();
        boolean hasSource = item.source() != null && !item.source().isBlank();

        if (hasDate && hasSource) {
            md.append("- **Date**: ").append(item.date()).append(" | **Source**: ").append(item.source()).append("\n");
        } else if (hasDate) {
            md.append("- **Date**: ").append(item.date()).append("\n");
        } else if (hasSource) {
            md.append("- **Source**: ").append(item.source()).append("\n");
        }
    }

    /**
     * Appends action items as Markdown checklist checkboxes.
     *
     * @param md          The {@link StringBuilder} accumulating the Markdown content.
     * @param actionItems The list of recommended action items or tasks.
     */
    private void appendItemActionItems(StringBuilder md, List<String> actionItems) {
        if (actionItems == null || actionItems.isEmpty()) {
            return;
        }
        md.append("- **Action Items**:\n");
        for (String action : actionItems) {
            md.append("  - [ ] ").append(action).append("\n");
        }
    }

    /**
     * Appends an error message block when a tool change query fails or times out.
     *
     * @param md     The {@link StringBuilder} accumulating the Markdown content.
     * @param result The failed tool result containing the error message.
     */
    private void appendToolError(StringBuilder md, ToolChangeResult result) {
        String error = result.errorMessage() != null ? result.errorMessage() : "Failed to retrieve updates.";
        md.append("⚠️ **Error**: ").append(error).append("\n\n");
    }

    /**
     * Writes the Markdown string to the default output file on disk.
     *
     * @param content The complete Markdown text to save.
     */
    private void saveToFile(String content) {
        try {
            Path path = Paths.get(DEFAULT_OUTPUT_FILE);
            Files.writeString(path, content);
            log.info("Successfully generated and saved Markdown report to: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save Markdown report file: {}", e.getMessage(), e);
        }
    }

    /**
     * Maps an impact level string to an emoji and formatted label.
     *
     * @param impactLevel The raw impact level (e.g., "CRITICAL", "MAJOR", "MINOR").
     * @return Formatted emoji and text badge string.
     */
    private String getImpactEmoji(String impactLevel) {
        if (impactLevel == null) {
            return "🔵 [MINOR]";
        }
        return switch (impactLevel.trim().toUpperCase()) {
            case "CRITICAL" -> "🔴 [CRITICAL]";
            case "MAJOR" -> "🟠 [MAJOR]";
            case "MINOR" -> "🔵 [MINOR]";
            default -> "🔹 [" + impactLevel + "]";
        };
    }
}
