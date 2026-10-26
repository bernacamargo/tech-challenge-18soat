package br.com.grupo117.oficina.identidadeacesso.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Perfil (papel de acesso; no Keycloak, realm role) de um usuario administrativo da
 * oficina (ADR 0006). So estes tres nomes, exatamente como escritos, sao perfis da
 * oficina; realm roles tecnicos do provedor de identidade sao ignorados.
 */
public enum Perfil {
    ATENDENTE,
    MECANICO,
    ADMINISTRADOR;

    /** Perfil com o nome exato informado; vazio para nulo, caixa diferente ou nome desconhecido. Nunca lanca. */
    public static Optional<Perfil> deNome(String nome) {
        for (Perfil perfil : values()) {
            if (perfil.name().equals(nome)) {
                return Optional.of(perfil);
            }
        }
        return Optional.empty();
    }

    /** Perfis da oficina presentes em nomes; ignora nulos e desconhecidos. Colecao nula vira conjunto vazio. Retorno imutavel. */
    public static Set<Perfil> reconhecer(Collection<String> nomes) {
        if (nomes == null) {
            return Set.of();
        }
        Set<Perfil> reconhecidos = EnumSet.noneOf(Perfil.class);
        for (String nome : nomes) {
            deNome(nome).ifPresent(reconhecidos::add);
        }
        return Set.copyOf(reconhecidos);
    }
}
