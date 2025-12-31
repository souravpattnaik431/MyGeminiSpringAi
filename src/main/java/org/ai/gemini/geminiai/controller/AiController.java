package org.ai.gemini.geminiai.controller;

import lombok.RequiredArgsConstructor;
import org.ai.gemini.geminiai.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import module java.base;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini")
public class AiController {

    private final AiService aiService;

    @GetMapping("/{toolName}")
    public String getToolChanges(@PathVariable String toolName) {
        return aiService.getToolChanges(toolName);
    }

    @GetMapping("/getAllToolChanges")
    public List<String> getAllToolChanges() {
        List<String> toolsList=new ArrayList<>();
        toolsList.add("azure-devops");
        toolsList.add("playwright");
        toolsList.add("saucelabs");
        toolsList.add("selenium-java");
        toolsList.add("spring-boot");
        return aiService.getAllToolChanges(toolsList);
    }
}
