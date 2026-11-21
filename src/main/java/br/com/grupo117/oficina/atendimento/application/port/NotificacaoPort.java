package br.com.grupo117.oficina.atendimento.application.port;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;

/** Agenda o e-mail da situação atual da OS na mesma transação; a entrega ocorre após o commit. */
@FunctionalInterface
public interface NotificacaoPort {
    void agendar(OrdemDeServico ordem);
}
