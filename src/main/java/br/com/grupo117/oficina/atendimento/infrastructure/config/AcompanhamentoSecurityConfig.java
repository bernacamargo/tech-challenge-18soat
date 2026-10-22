package br.com.grupo117.oficina.atendimento.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Libera apenas leitura do acompanhamento; demais APIs seguem a configuração existente. */
@Configuration
public class AcompanhamentoSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain acompanhamentoSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/acompanhamento/ordens-de-servico/*")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/acompanhamento/ordens-de-servico/*").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
