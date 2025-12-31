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
                        null,
                        null,
                        processingTime,
                        "ERROR");
            }

            Integer inputTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
            Integer outputTokens = chatResponse.getMetadata().getUsage().getCompletionTokens();
            String outputFromAI = chatResponse.getResult().getOutput().getText();

            log.info("Total input tokens for {}: {}", toolName, inputTokens);
            log.info("Total output tokens for {}: {}", toolName, outputTokens);
            log.info("Processing time for {}: {}ms", toolName, processingTime);

            return new ToolChangeResult(
                    toolName,
                    outputFromAI,
                    inputTokens,
                    outputTokens,
                    processingTime,
                    "SUCCESS");

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Exception in getting chat response from AI for tool {}: {}", toolName, e.getMessage());
            return new ToolChangeResult(
                    toolName,
                    "Error: " + e.getMessage(),
                    null,
                    null,
                    processingTime,
                    "ERROR");
        }
    }

    public ToolChangesResponse getAllToolChanges(List<String> tools) {
        long startTime = System.currentTimeMillis();
        List<ToolChangeResult> allResults = new ArrayList<>();

        for (int i = 0; i < tools.size(); i += batchSize) {
            List<String> batch = tools.subList(i, Math.min(i + batchSize, tools.size()));

            List<CompletableFuture<ToolChangeResult>> futures = batch.stream()
                    .map(tool -> CompletableFuture.supplyAsync(() -> getToolChangesWithMetadata(tool), executorService)
                            .completeOnTimeout(
                                    new ToolChangeResult(
                                            tool,
                                            "Request timed out",
                                            null,
                                            null,
                                            individualTimeout * 1000,
                                            "TIMEOUT"),
                                    individualTimeout,
                                    TimeUnit.SECONDS))
                    .toList();

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(batchTimeout, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Batch partial failure: {}", e.getMessage());
            }

            allResults.addAll(futures.stream().map(f -> f.getNow(
                    new ToolChangeResult(
                            "UNKNOWN",
                            "Failed to retrieve result",
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

        log.info("Total processing time for {} tools: {}ms", tools.size(), totalTime);
        log.info("Total input tokens: {}, Total output tokens: {}", totalInputTokens, totalOutputTokens);

        return new ToolChangesResponse(
                allResults,
                tools.size(),
                totalTime,
                totalInputTokens,
                totalOutputTokens,
                LocalDateTime.now().toString());
    }

}
