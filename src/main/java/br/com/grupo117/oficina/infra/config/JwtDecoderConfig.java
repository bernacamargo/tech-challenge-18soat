package br.com.grupo117.oficina.infra.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Valida o JWT do Keycloak sob demanda. Nos testes, um segredo HMAC substitui o issuer
 * para nao depender do Keycloak no ar.
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${oficina.security.jwt-secret:}") String segredo,
            @Value("${oficina.security.issuer-uri:http://localhost:8081/realms/oficina}") String issuer
    ) {
        if (segredo.isBlank()) {
            return new IssuerJwtDecoder(issuer);
        }
        SecretKey chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(chave).build();
    }

    static final class IssuerJwtDecoder implements JwtDecoder {

        private final String issuer;
        private volatile JwtDecoder delegate;

        IssuerJwtDecoder(String issuer) {
            this.issuer = issuer;
        }

        @Override
        public Jwt decode(String token) {
            JwtDecoder atual = delegate;
            if (atual == null) {
                synchronized (this) {
                    atual = delegate;
                    if (atual == null) {
                        atual = JwtDecoders.fromIssuerLocation(issuer);
                        delegate = atual;
                    }
                }
            }
            return atual.decode(token);
        }
    }
}
