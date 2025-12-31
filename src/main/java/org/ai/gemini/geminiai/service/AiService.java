package org.ai.gemini.geminiai.service;

import module java.base;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    public String getToolChanges(String toolName) {
        log.info("Tool name is: {}", toolName);
        Resource toolPrompt = resourceLoader.getResource("classpath:prompts/" + toolName + ".txt");
        PromptTemplate promptTemplate = new PromptTemplate(toolPrompt);
        Prompt prompt = promptTemplate.create(Map.of("days", days));
        log.info("Final Input Prompt is {}", prompt.getContents());
        ChatResponse chatResponse;
        try {
            chatResponse = chatClient.prompt(prompt).call().chatResponse();
        } catch (Exception e) {
            log.error("Exception in getting chat response from AI {}",e.getMessage());
            throw new RuntimeException("Exception in getting response from AI: " + e.getMessage());
        }
        if (chatResponse == null) {
            log.error("Empty chat response for tool: {}", toolName);
            return "Empty Chat Response from AI";
        } else {
            Integer inputTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
            Integer outputTokens = chatResponse.getMetadata().getUsage().getCompletionTokens();
            log.info("Total input tokens for {}: {}", toolName, inputTokens);
            log.info("Total output tokens for {}: {}", toolName, outputTokens);
            String outputFromAI = chatResponse.getResult().getOutput().getText();
            log.info("outputFromAI is {}",outputFromAI);
            return outputFromAI;
        }
    }
    public List<String> getAllToolChanges(List<String> tools) {
        long startTime = System.currentTimeMillis();
        List<String> allResults = new ArrayList<>();

        for (int i = 0; i < tools.size(); i += batchSize) {
            List<String> batch = tools.subList(i, Math.min(i + batchSize, tools.size()));

            List<CompletableFuture<String>> futures = batch.stream()
                    .map(tool -> CompletableFuture.supplyAsync(() -> getToolChanges(tool), executorService)
                            .completeOnTimeout("TIMEOUT", individualTimeout, TimeUnit.SECONDS))
                    .toList();

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(batchTimeout, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("Batch partial failure: " + e.getMessage());
            }

            allResults.addAll(futures.stream().map(f -> f.getNow("UNKNOWN")).toList());
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Total processing time for " + tools.size() + " tools: " + totalTime + "ms");

        return allResults;
    }

}
