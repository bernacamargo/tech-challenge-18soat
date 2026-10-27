package br.com.grupo117.oficina.infra.config;

import java.time.Instant;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;

/**
 * Validacoes do access token alem das do Spring Boot (issuer, audience, assinatura, tempo):
 * exige exp (o JwtTimestampValidator aceita token sem exp) e recusa ID token do Keycloak
 * (claim typ diferente de Bearer). O Boot soma estes beans ao JwtDecoder das propriedades;
 * nao declarar outro JwtDecoder.
 */
@Configuration
public class JwtValidacaoConfig {

    @Bean
    OAuth2TokenValidator<Jwt> exigeExpiracao() {
        return new JwtClaimValidator<Instant>(JwtClaimNames.EXP, Objects::nonNull);
    }

    @Bean
    OAuth2TokenValidator<Jwt> somenteTokenDeAcesso() {
        return new JwtClaimValidator<String>("typ", "Bearer"::equals);
    }
}
