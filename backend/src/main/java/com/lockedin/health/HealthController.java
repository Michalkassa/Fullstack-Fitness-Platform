package com.lockedin.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal example endpoint, written test-first (see HealthControllerTest).
 * The frontend doesn't depend on it; it's here as a template for the TDD loop.
 */
@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
