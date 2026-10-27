package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import br.com.grupo117.oficina.identidadeacesso.application.usecase.AutenticarUsuarioAdministrativo;
import br.com.grupo117.oficina.identidadeacesso.domain.Credencial;
import br.com.grupo117.oficina.identidadeacesso.domain.CredencialInvalidaException;
import br.com.grupo117.oficina.identidadeacesso.domain.UsuarioAdministrativo;
import br.com.grupo117.oficina.identidadeacesso.domain.UsuarioSemPerfilDaOficinaException;
import java.util.Objects;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * Resource server -> caso de uso de autenticacao administrativa (D4). Nao e @Bean: entra so na
 * cadeia do SecurityConfig. Credencial invalida: 401 com descricao fixa; sem perfil da oficina:
 * autenticado sem authorities (403 pelo piso); falha tecnica do provedor: 5xx. Nao loga.
 */
public final class UsuarioAdministrativoAuthenticationManager implements AuthenticationManager {

    static final String TOKEN_INVALIDO = "Token invalido";
    static final String FALHA_AO_VALIDAR = "Falha ao validar token";

    private final AutenticarUsuarioAdministrativo autenticar;

    public UsuarioAdministrativoAuthenticationManager(AutenticarUsuarioAdministrativo autenticar) {
        this.autenticar = Objects.requireNonNull(autenticar, "Caso de uso de autenticacao e obrigatorio");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof BearerTokenAuthenticationToken bearer)) {
            throw new InvalidBearerTokenException(TOKEN_INVALIDO);
        }
        try {
            UsuarioAdministrativo usuario = autenticar.autenticar(new Credencial(bearer.getToken()));
            return UsuarioAdministrativoAuthentication.de(usuario);
        } catch (CredencialInvalidaException e) {
            throw new InvalidBearerTokenException(TOKEN_INVALIDO, e);
        } catch (UsuarioSemPerfilDaOficinaException e) {
            return UsuarioAdministrativoAuthentication.semPerfilDaOficina(e.sujeito());
        } catch (JwtException e) {
            throw new AuthenticationServiceException(FALHA_AO_VALIDAR, e);
        }
    }
}
