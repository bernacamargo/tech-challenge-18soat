package br.com.grupo117.oficina.infra.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Coleta técnica na porta de management isolada; não altera a autorização das APIs. */
@Configuration(proxyBeanMethods = false)
@Profile("observability")
public class ObservabilitySecurityConfig {
    @Bean
    @Order(0)
    SecurityFilterChain prometheusSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.to(PrometheusScrapeEndpoint.class))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
