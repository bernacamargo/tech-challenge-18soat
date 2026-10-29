package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import br.com.grupo117.oficina.identidadeacesso.domain.Credencial;
import br.com.grupo117.oficina.identidadeacesso.domain.CredencialInvalidaException;
import br.com.grupo117.oficina.identidadeacesso.domain.DadosDaCredencial;
import br.com.grupo117.oficina.identidadeacesso.domain.ValidadorDeCredencial;
import java.util.Objects;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Adapter do port ValidadorDeCredencial: valida o access token com o JwtDecoder do Spring Boot. */
public final class KeycloakValidadorDeCredencial implements ValidadorDeCredencial {

    static final String CREDENCIAL_INVALIDA = "Credencial invalida";
    static final String CREDENCIAL_AUSENTE = "Credencial ausente";

    private final JwtDecoder decoder;

    public KeycloakValidadorDeCredencial(JwtDecoder decoder) {
        this.decoder = Objects.requireNonNull(decoder, "Decoder de JWT e obrigatorio");
    }

    @Override
    public DadosDaCredencial validar(Credencial credencial) {
        if (credencial == null) {
            throw new CredencialInvalidaException(CREDENCIAL_AUSENTE);
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(credencial.valor());
        } catch (BadJwtException e) {
            throw new CredencialInvalidaException(CREDENCIAL_INVALIDA, e);
        }
        return new DadosDaCredencial(jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                KeycloakRealmRoles.extrair(jwt));
    }
}
