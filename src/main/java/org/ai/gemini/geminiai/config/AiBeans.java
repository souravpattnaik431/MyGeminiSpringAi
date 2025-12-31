package org.ai.gemini.geminiai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiBeans {

    @Bean
    public ChatClient getChatClient(ChatClient.Builder builder)
    {
        return builder.build();
    }
}
