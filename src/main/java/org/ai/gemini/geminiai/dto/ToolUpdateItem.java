package org.ai.gemini.geminiai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Record representing a single structured update item for a tool.
 *
 * @param title       Title or headline of the update
 * @param date        Date or release timeframe of the update
 * @param source      Authoritative source or reference (e.g., GitHub Releases, Official Blog)
 * @param impactLevel Impact level of the update: Critical, Major, or Minor
 * @param summary     Brief summary of the update in 2-3 sentences
 * @param actionItems Action items or migration recommendations for engineers
 */
public record ToolUpdateItem(
        @JsonPropertyDescription("Title or headline of the update")
        String title,

        @JsonPropertyDescription("Date or release timeframe of the update (e.g., 'March 2026', '2026-02-28')")
        String date,

        @JsonPropertyDescription("Authoritative source or reference link/origin (e.g., 'GitHub Release v2.5.0', 'Official Blog')")
        String source,

        @JsonPropertyDescription("Impact level of the update: Critical, Major, or Minor")
        String impactLevel,

        @JsonPropertyDescription("Brief summary of the update in 2-3 sentences")
        String summary,

        @JsonPropertyDescription("Action items or migration recommendations for engineers")
        List<String> actionItems
) {
}
