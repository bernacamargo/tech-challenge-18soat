package br.com.grupo117.oficina.identidadeacesso.domain;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dados que o provedor de identidade afirma sobre o portador de uma credencial ja
 * verificada: sujeito (identificador estavel), login e os nomes de perfis informados
 * (no Keycloak, realm roles) como o provedor os entrega, sem filtro. Nao carrega dados
 * pessoais alem do login.
 * Sujeito e login nao sao validados aqui: as invariantes ficam em UsuarioAdministrativo.
 */
public record DadosDaCredencial(String sujeito, String login, Set<String> perfisInformados) {

    /** perfisInformados null vira Set.of(); elementos null sao descartados antes da copia;
     *  resultado imutavel. Sujeito e login guardados como recebidos. */
    public DadosDaCredencial {
        perfisInformados = perfisInformados == null
                ? Set.of()
                : perfisInformados.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }
}
