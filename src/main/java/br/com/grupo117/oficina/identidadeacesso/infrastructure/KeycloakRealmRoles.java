package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

/** Le os realm roles do Keycloak (claim realm_access.roles) sem interpretar: quem filtra e o dominio. */
final class KeycloakRealmRoles {

    private KeycloakRealmRoles() {
    }

    static Set<String> extrair(Jwt jwt) {
        Objects.requireNonNull(jwt, "Jwt e obrigatorio");
        Object bruto = jwt.getClaims().get("realm_access");
        if (!(bruto instanceof Map<?, ?> mapa)) {
            return Set.of();
        }
        if (!(mapa.get("roles") instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }
}
