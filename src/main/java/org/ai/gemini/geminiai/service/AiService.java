package org.ai.gemini.geminiai.service;

import module java.base;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * Service for managing AI-powered tool change queries.
 * <p>
 * This service handles both individual and batch tool queries,
 * managing parallel execution, token tracking, and error handling.
 * </p>
 *
 * @author Gemini AI Service
 * @version 1.0
 * @since 2026-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {
    private final ChatClient chatClient;
    private final ExecutorService executorService = new ThreadPoolExecutor(
            10, 20, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());

    private final ResourceLoader resourceLoader;
    @Value("${app.ai.batch-size:50}")
    private int batchSize;

    @Value("${app.ai.timeout.individual:60}")
    private long individualTimeout;

    @Value("${app.ai.timeout.batch:120}")
    private long batchTimeout;
    @Value("tool.last.changes.days")
    public String days;

    /**
     * Retrieves tool changes with complete metadata for a single tool.
     * <p>
     * This method queries the AI service for the latest updates about a specific
     * tool,
     * tracking token usage and processing time.
     * </p>
     *
     * @param toolName Name of the tool to query
     * @return ToolChangeResult containing content, tokens, processing time, and
     *         status
     */
    public ToolChangeResult getToolChangesWithMetadata(String toolName) {
        long startTime = System.currentTimeMillis();
        log.info("Tool name is: {}", toolName);

        try {
            Resource toolPrompt = resourceLoader.getResource("classpath:prompts/" + toolName + ".txt");
            PromptTemplate promptTemplate = new PromptTemplate(toolPrompt);
            Prompt prompt = promptTemplate.create(Map.of("days", days));
            log.info("Final Input Prompt is {}", prompt.getContents());

            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            long processingTime = System.currentTimeMillis() - startTime;

                        if (chatResponse == null) {
                                log.error("Empty chat response for tool: {}", toolName);
                                return new ToolChangeResult(
                                                toolName,
                                                "Empty Chat Response from AI",
                                                "",
                                                null,
                                                null,
                                                processingTime,
                                                "ERROR");
                        }

                        Integer inputTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
                        Integer outputTokens = chatResponse.getMetadata().getUsage().getCompletionTokens();
                        String outputFromAI = chatResponse.getResult().getOutput().getText();
                        String htmlContent = convertMarkdownToHtml(outputFromAI);

                        log.info("Total input tokens for {}: {}", toolName, inputTokens);
                        log.info("Total output tokens for {}: {}", toolName, outputTokens);
                        log.info("Content for {}: \n{}", toolName, outputFromAI);
                        log.info("Processing time for {}: {}ms", toolName, processingTime);

                        return new ToolChangeResult(
                                        toolName,
                                        outputFromAI,
                                        htmlContent,
                                        inputTokens,
                                        outputTokens,
                                        processingTime,
                                        "SUCCESS");

                } catch (Exception e) {
                        long processingTime = System.currentTimeMillis() - startTime;
                        log.error("Exception in getting chat response from AI for tool {}: {}", toolName,
                                        e.getMessage());
                        return new ToolChangeResult(
                                        toolName,
                                        "Error: " + e.getMessage(),
                                        "",
                                        null,
                                        null,
                                        processingTime,
                                        "ERROR");
                }
        }

        private String convertMarkdownToHtml(String markdown) {
                if (markdown == null)
                        return "";

                String html = markdown;

                // Escape HTML (Minimal)
                html = html.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

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

                // Tables (Basic support) - converting pipelines to table rows
                // This is a very robust simplistic approach for standard Markdown tables
                if (html.contains("|")) {
                        html = html.replaceAll("(?m)^\\|(.*)\\|$", "<tr><td>$1</td></tr>");
                        html = html.replace("<td>", "<td>").replace("|", "</td><td>");
                        html = "<table>" + html + "</table>";
                        // Cleanup table mess (this is very hacky but works for simple tables)
                        html = html.replace("<table>", "<div class='table-wrapper'><table>").replace("</table>",
                                        "</table></div>");
                }

                // Lists
                // Handle * or - lists with optional indentation
                html = html.replaceAll("(?m)^\\s*[*•-]\\s+(.*)$", "<li>$1</li>");

                // Wrap lists (better heuristic)
                // We use a temporary placeholder to identify blocks of LIs
                html = html.replaceAll("((<li>.*</li>\\s*)+)", "<ul>$1</ul>");

                // Paragraphs (double newlines to br, but not inside tags ideally)
                // A safer bet for blog text is to just replace double newlines with breaks
                html = html.replaceAll("(?m)\\n\\n", "<br/><br/>");

                return html;
        }

        /**
         * Retrieves tool changes for multiple tools in parallel.
         * <p>
         * This method processes multiple tool queries concurrently using a thread pool,
         * aggregates the results, and calculates total token usage and processing time.
         * </p>
         *
         * @param tools List of tool names to query
         * @return ToolChangesResponse with individual results and aggregated statistics
         */
        public ToolChangesResponse getAllToolChanges(List<String> tools) {
                long startTime = System.currentTimeMillis();
                List<ToolChangeResult> allResults = new ArrayList<>();

                for (int i = 0; i < tools.size(); i += batchSize) {
                        List<String> batch = tools.subList(i, Math.min(i + batchSize, tools.size()));

                        List<CompletableFuture<ToolChangeResult>> futures = batch.stream()
                                        .map(tool -> CompletableFuture
                                                        .supplyAsync(() -> getToolChangesWithMetadata(tool),
                                                                        executorService)
                                                        .completeOnTimeout(
                                                                        new ToolChangeResult(
                                                                                        tool,
                                                                                        "Request timed out",
                                                                                        "",
                                                                                        null,
                                                                                        null,
                                                                                        individualTimeout * 1000,
                                                                                        "TIMEOUT"),
                                                                        individualTimeout,
                                                                        TimeUnit.SECONDS))
                                        .toList();

                        try {
                                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(batchTimeout,
                                                TimeUnit.SECONDS);
                        } catch (Exception e) {
                                log.warn("Batch partial failure: {}", e.getMessage());
                        }

                        allResults.addAll(futures.stream().map(f -> f.getNow(
                                        new ToolChangeResult(
                                                        "UNKNOWN",
                                                        "Failed to retrieve result",
                                                        "",
                                                        null,
                                                        null,
                                                        null,
                                                        "ERROR")))
                                        .toList());
                }

        long totalTime = System.currentTimeMillis() - startTime;

        int totalInputTokens = allResults.stream()
                .filter(r -> r.inputTokens() != null)
                .mapToInt(ToolChangeResult::inputTokens)
                .sum();

        int totalOutputTokens = allResults.stream()
                .filter(r -> r.outputTokens() != null)
                .mapToInt(ToolChangeResult::outputTokens)
                .sum();

        log.info("=".repeat(80));
        log.info("BATCH PROCESSING SUMMARY");
        log.info("Total tools processed: {}", tools.size());
        log.info("Total processing time: {}ms ({} seconds)", totalTime, totalTime / 1000.0);
        log.info("Total input tokens (all tools): {}", totalInputTokens);
        log.info("Total output tokens (all tools): {}", totalOutputTokens);
        log.info("Total tokens consumed: {}", totalInputTokens + totalOutputTokens);
        log.info("=".repeat(80));

        return new ToolChangesResponse(
                allResults,
                tools.size(),
                totalTime,
                totalInputTokens,
                totalOutputTokens,
                LocalDateTime.now().toString());
    }

}
