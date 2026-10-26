package br.com.grupo117.oficina.identidadeacesso.application.usecase;

import br.com.grupo117.oficina.identidadeacesso.domain.Credencial;
import br.com.grupo117.oficina.identidadeacesso.domain.CredencialInvalidaException;
import br.com.grupo117.oficina.identidadeacesso.domain.DadosDaCredencial;
import br.com.grupo117.oficina.identidadeacesso.domain.UsuarioAdministrativo;
import br.com.grupo117.oficina.identidadeacesso.domain.ValidadorDeCredencial;
import java.util.Objects;

/**
 * Autentica o usuario administrativo a partir da credencial (access token Bearer):
 * valida pelo port ValidadorDeCredencial uma unica vez e monta a identidade pelo dominio.
 * Nao persiste nada (sem transacao), nao loga e nao captura excecoes.
 */
public final class AutenticarUsuarioAdministrativo {

    private final ValidadorDeCredencial validador;

    public AutenticarUsuarioAdministrativo(ValidadorDeCredencial validador) {
        this.validador = Objects.requireNonNull(validador, "Validador de credencial e obrigatorio");
    }

    /**
     * @throws CredencialInvalidaException credencial ausente, recusada pelo validador ou sem id/login
     * @throws br.com.grupo117.oficina.identidadeacesso.domain.UsuarioSemPerfilDaOficinaException
     *         credencial valida sem perfil da oficina
     * @throws RuntimeException falha tecnica do provedor, propagada sem embrulhar
     */
    public UsuarioAdministrativo autenticar(Credencial credencial) {
        if (credencial == null) {
            throw new CredencialInvalidaException("Credencial ausente");
        }
        DadosDaCredencial dados = validador.validar(credencial);
        return UsuarioAdministrativo.de(dados);
    }
}
