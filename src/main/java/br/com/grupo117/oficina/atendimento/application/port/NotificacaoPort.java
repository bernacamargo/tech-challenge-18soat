package br.com.grupo117.oficina.atendimento.application.port;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;

/** Agenda um snapshot do orçamento na mesma transação; a entrega ocorre após o commit. */
@FunctionalInterface
public interface NotificacaoPort {
    void agendarOrcamento(OrdemDeServico ordem);
}
