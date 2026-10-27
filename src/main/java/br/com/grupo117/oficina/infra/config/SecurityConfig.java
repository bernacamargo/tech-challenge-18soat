package br.com.grupo117.oficina.infra.config;

import br.com.grupo117.oficina.identidadeacesso.application.usecase.AutenticarUsuarioAdministrativo;
import br.com.grupo117.oficina.identidadeacesso.infrastructure.UsuarioAdministrativoAuthenticationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguranca da API: stateless, CSRF off (API REST). Health, Swagger e /error sao publicos.
 * /error precisa ser anonimo para uma falha na aprovacao publica continuar respondendo 500.
 * Consulta, aprovacao e rejeicao da OS pelo UUID sao publicas nesta mesma cadeia (ADR 0015).
 * As rotas administrativas exigem JWT do Keycloak (ADR 0006) com um perfil da oficina:
 * clientes e veiculos por regra de URL (nega antes da validacao do corpo); demais operacoes
 * por @PreAuthorize. Qualquer outra rota exige ao menos um perfil da oficina (piso).
 * Esta e a unica declaracao de @EnableMethodSecurity da aplicacao.
 * A autenticacao delega ao caso de uso AutenticarUsuarioAdministrativo (ADR 0006).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AutenticarUsuarioAdministrativo autenticarUsuarioAdministrativo)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/api/health", "/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/acompanhamento/ordens-de-servico/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/acompanhamento/ordens-de-servico/*/aprovacao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/acompanhamento/ordens-de-servico/*/rejeicao").permitAll()
                        .requestMatchers("/api/clientes", "/api/clientes/**", "/api/veiculos", "/api/veiculos/**")
                        .hasAnyRole("ATENDENTE", "ADMINISTRADOR")
                        .anyRequest().hasAnyRole("ATENDENTE", "MECANICO", "ADMINISTRADOR"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.authenticationManager(
                        new UsuarioAdministrativoAuthenticationManager(autenticarUsuarioAdministrativo))))
                .build();
    }
}
