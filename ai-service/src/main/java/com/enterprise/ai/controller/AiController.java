package com.enterprise.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("service", "ai-service", "status", "ready");
    }
}
