package org.ai.gemini.geminiai.service;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import static org.ai.gemini.geminiai.constants.HtmlConstants.DIV_NEWLINE;

@Service
public class CreateHtmlReport {
    /**
     * convert the AI Markdown output to HTML file
     * @param markdown mention the Markdown output
     * @return HTML output
     */
    public String convertMarkdownToHtml(String markdown) {
        if (markdown == null)
            return "";

        String html = markdown;

        // Escape HTML (Minimal) - but preserve some Markdown for later processing
        html = html.replace("&", "&amp;");

        // Process Markdown tables FIRST and convert them to div-based layout (NO HTML
        // TABLES)
        html = convertMarkdownTablesToDivs(html);

        // Now escape remaining HTML
        html = html.replace("<", "&lt;").replace(">", "&gt;");

        // But restore our div-based table markup
        html = html.replace("&lt;div class='md-table'&gt;", "<div class='md-table'>")
                .replace("&lt;/div&gt;", "</div>")
                .replace("&lt;div class='md-table-row'&gt;", "<div class='md-table-row'>")
                .replace("&lt;div class='md-table-cell'&gt;", "<div class='md-table-cell'>")
                .replace("&lt;div class='md-table-header'&gt;", "<div class='md-table-header'>");

        // Headers
        html = html.replaceAll("(?m)^### (.*)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^## (.*)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^# (.*)$", "<h1>$1</h1>");

        // Bold
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        // Code Blocks
        html = html.replaceAll("(?s)```(.*?)\\n(.*?)```", "<pre><code>$2</code></pre>");

        // Inline Code
        html = html.replaceAll("`(.*?)`", "<code>$1</code>");

        // Lists
        // Handle * or - lists with optional indentation
        html = html.replaceAll("(?m)^\\s*[*•-]\\s+(.*)$", "<li>$1</li>");

        // Wrap lists (better heuristic)
        html = html.replaceAll("((<li>.*</li>\\s*)+)", "<ul>$1</ul>");

        // Paragraphs (double newlines to br, but not inside tags ideally)
        html = html.replaceAll("(?m)\\n\\n", "<br/><br/>");

        return html;
    }

    /**
     * Converts Markdown tables to div-based layout instead of HTML tables.
     * This creates cleaner, more flexible layouts that don't look ugly.
     */
    public String convertMarkdownTablesToDivs(String markdown) {
        String markdown1 = getMarkDown(markdown);
        if (markdown1 != null) return markdown1;

        StringBuilder result = new StringBuilder();
        String[] lines = markdown.split("\\n");
        boolean inTable = false;
        boolean isFirstRow = true;

        for (String s : lines) {
            String line = s.trim();

            // Check if this is a table row (contains |)
            if (line.startsWith("|") && line.endsWith("|")) {
                // Skip separator rows (like |---|---|)
                if (line.matches("^\\|[\\s:-]+\\|$")
                        || line.matches("^\\|\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|$")) {
                    isFirstRow = false;
                    continue;
                }

                if (!inTable) {
                    result.append("<div class='md-table'>\n");
                    inTable = true;
                    isFirstRow = true;
                }

                // Parse table cells
                String[] cells = line.split("\\|");
                String rowClass = isFirstRow ? "md-table-header" : "md-table-row";
                result.append("<div class='").append(rowClass).append("'>\n");

                appendTable(cells, result);

                result.append(DIV_NEWLINE);
                isFirstRow = false;

            } else {
                // Not a table row
                if (inTable) {
                    result.append(DIV_NEWLINE); // Close the table
                    inTable = false;
                    isFirstRow = true;
                }
                result.append(line).append("\n");
            }
        }

        // Close table if still open
        if (inTable) {
            result.append(DIV_NEWLINE);
        }

        return result.toString();
    }

    /**
     * return Markdown if contains "|"
     * @param markdown mention Markdown
     * @return MarkDown
     */
    public static @Nullable String getMarkDown(String markdown) {
        if (!markdown.contains("|")) {
            return markdown;
        }
        return null;
    }

    /**
     * append table
     * @param cells mention cells
     * @param result mention result
     */
    public void appendTable(String[] cells, StringBuilder result) {
        for (String cell : cells) {
            cell = cell.trim();
            if (!cell.isEmpty()) {
                result.append("<div class='md-table-cell'>").append(cell)
                        .append(DIV_NEWLINE);
            }
        }
    }
}
