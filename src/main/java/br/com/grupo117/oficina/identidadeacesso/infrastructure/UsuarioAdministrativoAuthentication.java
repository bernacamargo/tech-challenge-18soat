package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import br.com.grupo117.oficina.identidadeacesso.domain.UsuarioAdministrativo;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Autenticacao de um usuario administrativo: authorities ROLE_ de cada Perfil nascem so aqui. Nao guarda o token. */
public final class UsuarioAdministrativoAuthentication extends AbstractAuthenticationToken {

    private final UsuarioAdministrativo usuario;
    private final String sujeito;

    private UsuarioAdministrativoAuthentication(UsuarioAdministrativo usuario, String sujeito,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.usuario = usuario;
        this.sujeito = sujeito;
        setAuthenticated(true);
    }

    /** Usuario com perfil da oficina: uma authority ROLE_ por Perfil; nome = sub. */
    public static UsuarioAdministrativoAuthentication de(UsuarioAdministrativo usuario) {
        Objects.requireNonNull(usuario, "Usuario administrativo e obrigatorio");
        List<SimpleGrantedAuthority> authorities = usuario.perfis().stream()
                .map(p -> new SimpleGrantedAuthority("ROLE_" + p.name()))
                .toList();
        return new UsuarioAdministrativoAuthentication(usuario, usuario.id(), authorities);
    }

    /** Credencial valida sem perfil da oficina: autenticado sem authorities (o piso da API responde 403). */
    public static UsuarioAdministrativoAuthentication semPerfilDaOficina(String sujeito) {
        Objects.requireNonNull(sujeito, "Sujeito e obrigatorio");
        return new UsuarioAdministrativoAuthentication(null, sujeito, List.of());
    }

    /** Nunca o token. */
    @Override
    public Object getCredentials() {
        return null;
    }

    /** UsuarioAdministrativo, ou null no caso sem perfil da oficina. */
    @Override
    public Object getPrincipal() {
        return usuario;
    }

    /** sub do token. */
    @Override
    public String getName() {
        return sujeito;
    }

    /** Sem sub, login nem detalhes: o filtro do Spring loga a autenticacao em DEBUG. */
    @Override
    public String toString() {
        return "UsuarioAdministrativoAuthentication[authorities=" + getAuthorities() + "]";
    }
}
