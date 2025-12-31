package org.ai.gemini.geminiai.controller;

import lombok.RequiredArgsConstructor;
import org.ai.gemini.geminiai.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini")
public class AiController {

    private final AiService aiService;


    @GetMapping("/playwright")
    public String getPlaywrightChanges() {
        return aiService.getPlaywrightChanges("30");
    }
}
