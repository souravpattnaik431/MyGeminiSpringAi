package org.ai.gemini.geminiai;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected @NonNull SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(GeminiAiApplication.class);
    }

}
