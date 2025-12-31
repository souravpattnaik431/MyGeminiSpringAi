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

    @Value("classpath:/prompts/playwright.txt")
    private Resource playwrightPrompt;
    private final ResourceLoader resourceLoader;


    public String readTexFileContent(String fileName){
        Resource resource = resourceLoader.getResource("classpath:last-responses/" + fileName);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("File not found for {} ",fileName);
        }
        return "";
    }

    public String getPlaywrightChanges(String days) {
        PromptTemplate promptTemplate=new PromptTemplate(playwrightPrompt);
        String lastResponse=readTexFileContent("playwright-response.txt");
        Prompt prompt=promptTemplate.create(Map.of("days",days,"lastResponse",lastResponse));
        ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
        if(chatResponse==null)
        {
            log.error("Empty chat response");
            return "";
        }
        else {
            Integer inputTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
            Integer outputTokens = chatResponse.getMetadata().getUsage().getCompletionTokens();
            log.info("Total input tokens {}",inputTokens);
            log.info("Total output tokens {}",outputTokens);
            return chatResponse.getResult().getOutput().getText();
        }
    }


}
