package br.com.grupo117.oficina.identidadeacesso.domain;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Usuario administrativo autenticado, montado a cada requisicao a partir da credencial
 * validada pelo provedor de identidade. Nao e persistido: a conta vive no Keycloak.
 * id = sub do token; login = preferred_username; perfis = perfis da oficina (nao vazio).
 */
public record UsuarioAdministrativo(String id, String login, Set<Perfil> perfis) {

    public UsuarioAdministrativo {
        if (id == null || id.isBlank()) {
            throw new CredencialInvalidaException("Credencial sem identificador");
        }
        if (login == null || login.isBlank()) {
            throw new CredencialInvalidaException("Credencial sem login");
        }
        perfis = perfis == null
                ? Set.of()
                : perfis.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        if (perfis.isEmpty()) {
            throw new UsuarioSemPerfilDaOficinaException(id);
        }
    }

    public static UsuarioAdministrativo de(DadosDaCredencial dados) {
        if (dados == null) {
            throw new CredencialInvalidaException("Credencial invalida");
        }
        return new UsuarioAdministrativo(dados.sujeito(), dados.login(), Perfil.reconhecer(dados.perfisInformados()));
    }

    public boolean possui(Perfil perfil) {
        return perfil != null && perfis.contains(perfil);
    }
}
