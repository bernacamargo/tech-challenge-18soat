package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Le os papeis do realm Keycloak (claim realm_access.roles) e os expoe como ROLE_*.
 */
public final class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        Object papeis = realmAccess.get("roles");
        if (!(papeis instanceof Collection<?> colecao)) {
            return List.of();
        }
        return colecao.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(papel -> new SimpleGrantedAuthority("ROLE_" + papel))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
