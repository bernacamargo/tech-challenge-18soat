package br.com.grupo117.oficina.atendimento.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Libera a consulta e a decisão do cliente; demais APIs seguem a configuração existente.
 * Não há cookie de sessão: o UUID da OS é a credencial, então o CSRF não se aplica a estas rotas.
 */
@Configuration
public class AcompanhamentoSecurityConfig {
    private static final String ROTAS_PUBLICAS = "/api/acompanhamento/ordens-de-servico/**";

    @Bean
    @Order(1)
    SecurityFilterChain acompanhamentoSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(ROTAS_PUBLICAS)
                .csrf(csrf -> csrf.ignoringRequestMatchers(ROTAS_PUBLICAS))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/acompanhamento/ordens-de-servico/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/acompanhamento/ordens-de-servico/*/aprovacao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/acompanhamento/ordens-de-servico/*/rejeicao").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
