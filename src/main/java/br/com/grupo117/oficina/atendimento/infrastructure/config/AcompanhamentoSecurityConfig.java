package br.com.grupo117.oficina.atendimento.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Libera a consulta e a decisão do cliente; demais APIs seguem a configuração existente. */
@Configuration
public class AcompanhamentoSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain acompanhamentoSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/acompanhamento/ordens-de-servico/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/acompanhamento/ordens-de-servico/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/acompanhamento/ordens-de-servico/*/aprovacao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/acompanhamento/ordens-de-servico/*/rejeicao").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
