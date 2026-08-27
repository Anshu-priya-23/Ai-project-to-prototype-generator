package com.enterprise.ai.controller;

import com.enterprise.ai.messaging.PrototypeRequest;
import com.enterprise.ai.messaging.PrototypeResult;
import com.enterprise.ai.service.PrototypeGenerator;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final PrototypeGenerator generator;

    public AiController(PrototypeGenerator generator) { this.generator = generator; }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("service", "ai-service", "status", "ready");
    }

    @PostMapping("/prototype")
    public PrototypeResult generate(@RequestBody PrototypeRequest request) {
        return generator.generate(request);
    }
}
