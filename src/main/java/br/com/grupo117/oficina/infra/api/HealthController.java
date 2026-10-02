package br.com.grupo117.oficina.infra.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint simples de health/app-info, complementar ao /actuator/health.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "app", "oficina"
        );
    }
}
