package org.ai.gemini.geminiai.service;

import lombok.extern.slf4j.Slf4j;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.ai.gemini.geminiai.dto.ToolUpdateItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to generate and save clean, formatted Markdown reports from structured AI responses.
 *
 * @author Gemini AI Service
 * @version 1.0
 * @since 2026-01-01
 */
@Service
@Slf4j
public class MarkdownReportService {

    private static final String DEFAULT_OUTPUT_FILE = "latest_tool_updates.md";

    /**
     * Generates a clean Markdown report from a ToolChangesResponse and saves it to disk.
     *
     * @param response Aggregated response containing individual tool change results.
     * @return Formatted Markdown string.
     */
    public String generateAndSaveReport(ToolChangesResponse response) {
        StringBuilder md = new StringBuilder();

        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Header
        md.append("# 🚀 Technical Tools Update Digest\n\n");
        md.append("> **Generated**: `").append(formattedTime).append("`  \n");
        md.append("> **Total Tools Processed**: `").append(response.totalTools()).append("` | ");
        md.append("**Total Processing Time**: `").append(response.totalProcessingTimeMs()).append(" ms`");

        if (response.totalInputTokens() != null && response.totalOutputTokens() != null) {
            md.append(" | **Total Tokens (In/Out)**: `")
              .append(response.totalInputTokens()).append(" / ").append(response.totalOutputTokens()).append("`");
        }
        md.append("\n\n---\n\n");

        if (response.results() == null || response.results().isEmpty()) {
            md.append("_No tool results available._\n");
        } else {
            for (ToolChangeResult result : response.results()) {
                md.append("## 📦 ").append(result.toolName().toUpperCase()).append("\n\n");

                md.append("**Status**: `").append(result.status()).append("`");
                if (result.processingTimeMs() != null) {
                    md.append(" | **Time**: `").append(result.processingTimeMs()).append(" ms`");
                }
                if (result.inputTokens() != null && result.outputTokens() != null) {
                    md.append(" | **Tokens**: `").append(result.inputTokens()).append(" in / ").append(result.outputTokens()).append(" out`");
                }
                md.append("\n\n");

                if (result.report() != null) {
                    if (result.report().overview() != null && !result.report().overview().isBlank()) {
                        md.append("> ").append(result.report().overview()).append("\n\n");
                    }

                    if (result.report().updates() != null && !result.report().updates().isEmpty()) {
                        for (ToolUpdateItem item : result.report().updates()) {
                            String impactEmoji = getImpactEmoji(item.impactLevel());
                            String title = (item.title() != null && !item.title().isBlank()) ? item.title() : "Update";

                            md.append("### ").append(impactEmoji).append(" ").append(title).append("\n\n");

                            final boolean b = item.source() != null && !item.source().isBlank();
                            if (item.date() != null && !item.date().isBlank()) {
                                md.append("- **Date**: ").append(item.date());
                                if (b) {
                                    md.append(" | **Source**: ").append(item.source());
                                }
                                md.append("\n");
                            } else if (b) {
                                md.append("- **Source**: ").append(item.source()).append("\n");
                            }

                            if (item.summary() != null && !item.summary().isBlank()) {
                                md.append("- **Summary**: ").append(item.summary()).append("\n");
                            }

                            if (item.actionItems() != null && !item.actionItems().isEmpty()) {
                                md.append("- **Action Items**:\n");
                                for (String action : item.actionItems()) {
                                    md.append("  - [ ] ").append(action).append("\n");
                                }
                            }
                            md.append("\n");
                        }
                    } else {
                        md.append("_No specific updates reported for this timeframe._\n\n");
                    }
                } else {
                    md.append("⚠️ **Error**: ").append(result.errorMessage() != null ? result.errorMessage() : "Failed to retrieve updates.").append("\n\n");
                }

                md.append("---\n\n");
            }
        }

        String markdownContent = md.toString();

        try {
            Path path = Paths.get(DEFAULT_OUTPUT_FILE);
            Files.writeString(path, markdownContent);
            log.info("Successfully generated and saved Markdown report to: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save Markdown report file: {}", e.getMessage(), e);
        }

        return markdownContent;
    }

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
