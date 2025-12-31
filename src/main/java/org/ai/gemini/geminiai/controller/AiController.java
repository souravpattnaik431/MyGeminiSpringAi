package org.ai.gemini.geminiai.controller;

import lombok.RequiredArgsConstructor;
import org.ai.gemini.geminiai.dto.ToolChangeResult;
import org.ai.gemini.geminiai.dto.ToolChangesRequest;
import org.ai.gemini.geminiai.dto.ToolChangesResponse;
import org.ai.gemini.geminiai.service.AiService;
import org.springframework.web.bind.annotation.*;

import module java.base;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini")
public class AiController {

    private final AiService aiService;

    @GetMapping("/{toolName}")
    public ToolChangeResult getToolChanges(@PathVariable String toolName) {
        return aiService.getToolChangesWithMetadata(toolName);
    }

    @PostMapping("/getAllToolChanges")
    public ToolChangesResponse getAllToolChanges(@RequestBody ToolChangesRequest request) {
        if (request.tools() == null || request.tools().isEmpty()) {
            throw new IllegalArgumentException("Tools list cannot be empty");
        }
        return aiService.getAllToolChanges(request.tools());
    }
}
