package org.ai.gemini.geminiai.dto;
import module java.base;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Record representing the overall structured report for a tool.
 *
 * @param overview High-level summary overview of the latest developments for this tool
 * @param updates  List of distinct news updates, releases, or announcements
 */
public record ToolReport(
        @JsonPropertyDescription("A high-level summary overview of the latest developments for this tool")
        String overview,

        @JsonPropertyDescription("List of distinct news updates, releases, or announcements")
        List<ToolUpdateItem> updates
) {
}
