package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import br.com.grupo117.oficina.identidadeacesso.application.usecase.AutenticarUsuarioAdministrativo;
import br.com.grupo117.oficina.identidadeacesso.domain.ValidadorDeCredencial;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Liga o adapter do Keycloak ao caso de uso de autenticacao administrativa, sem anotar
 * domain/application com Spring. O JwtDecoder e o unico do Spring Boot.
 */
@Configuration
public class IdentidadeAcessoConfig {

    @Bean
    ValidadorDeCredencial validadorDeCredencial(JwtDecoder jwtDecoder) {
        return new KeycloakValidadorDeCredencial(jwtDecoder);
    }

    @Bean
    AutenticarUsuarioAdministrativo autenticarUsuarioAdministrativo(ValidadorDeCredencial validadorDeCredencial) {
        return new AutenticarUsuarioAdministrativo(validadorDeCredencial);
    }
}
