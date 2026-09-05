package org.ai.gemini.geminiai.service;

import module java.base;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.ai.gemini.geminiai.dto.ToolReport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;


import static org.ai.gemini.geminiai.constants.AiConstants.ERROR;

/**
 * Service for managing AI-powered tool change queries with Spring AI structured output.
 * <p>
 * This service handles both individual and batch tool queries,
 * utilizing Spring AI's native structured output to produce typed DTOs directly from Gemini.
 * </p>
 *
 * @author Gemini AI Service
 * @version 2.0
 * @since 2026-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {
    private final ChatClient chatClient;
    private final ExecutorService executorService = new ThreadPoolExecutor(
            10, 20, 120L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());

    private final ResourceLoader resourceLoader;
    @Value("${app.ai.batch-size:50}")
    private int batchSize;

    @Value("${app.ai.timeout.individual:60}")
    private long individualTimeout;

    @Value("${app.ai.timeout.batch:120}")
    private long batchTimeout;
    @Value("${tool.last.changes.days:30}")
    public String days;

    /**
     * Retrieves structured tool changes with complete metadata for a single tool.
     *
     * @param toolName Name of the tool to query
     * @return ToolChangeResult containing structured ToolReport, token usage, processing time, and status
     */
    public ToolChangeResult getToolChangesWithMetadata(String toolName) {
        long startTime = System.currentTimeMillis();
        log.info("Tool name is: {}", toolName);

        try {
            Resource toolPrompt = resourceLoader.getResource("classpath:prompts/" + toolName + ".txt");
            PromptTemplate promptTemplate = new PromptTemplate(toolPrompt);
            Prompt prompt = promptTemplate.create(Map.of("days", days));
            log.info("Final Input Prompt is: {}", prompt.getContents());

            ResponseEntity<ChatResponse, ToolReport> responseEntity = chatClient.prompt(prompt)
                    .call()
                    .responseEntity(ToolReport.class, spec -> spec.useProviderStructuredOutput().validateSchema());

            long processingTime = System.currentTimeMillis() - startTime;
            ToolReport report = responseEntity.entity();
            ChatResponse chatResponse = responseEntity.response();

            Integer inputTokens = null;
            Integer outputTokens = null;
            if (chatResponse != null) {
                ChatResponseMetadata metadata = chatResponse.getMetadata();
                Usage usage = metadata.getUsage();
                inputTokens = usage.getPromptTokens();
                outputTokens = usage.getCompletionTokens();
            }

            log.info("Total input tokens for {}: {}", toolName, inputTokens);
            log.info("Total output tokens for {}: {}", toolName, outputTokens);
            log.info("Structured Report for {}: {}", toolName, report);
            log.info("Processing time for {}: {}ms", toolName, processingTime);

            return new ToolChangeResult(
                    toolName,
                    report,
                    inputTokens,
                    outputTokens,
                    processingTime,
                    "SUCCESS",
                    null);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Exception in getting structured response from AI for tool {}: {}", toolName, e.getMessage(), e);
            return new ToolChangeResult(
                    toolName,
                    null,
                    null,
                    null,
                    processingTime,
                    ERROR,
                    e.getMessage());
        }
    }

    /**
     * Retrieves tool changes for multiple tools in parallel.
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
                                            null,
                                            null,
                                            null,
                                            individualTimeout * 1000,
                                            "TIMEOUT",
                                            "Request timed out"),
                                    individualTimeout,
                                    TimeUnit.SECONDS))
                    .toList();

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(batchTimeout,
                        TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Batch partial failure: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }

            allResults.addAll(futures.stream().map(f -> f.getNow(
                            new ToolChangeResult(
                                    "UNKNOWN",
                                    null,
                                    null,
                                    null,
                                    null,
                                    ERROR,
                                    "Failed to retrieve result")))
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
                LocalDateTime.now(ZoneId.systemDefault()).toString());
    }

}
