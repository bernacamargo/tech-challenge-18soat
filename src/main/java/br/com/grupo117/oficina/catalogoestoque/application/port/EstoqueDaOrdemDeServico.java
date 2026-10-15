package br.com.grupo117.oficina.catalogoestoque.application.port;

import java.util.UUID;

/**
 * Port de entrada usado pelo contexto de atendimento para movimentar o estoque de
 * uma OS. Trabalha apenas com identificadores, sem expor o dominio de estoque nem
 * depender do dominio da OS.
 */
public interface EstoqueDaOrdemDeServico {

    /** Separa a quantidade da peca para a OS e devolve o id da reserva criada. */
    UUID reservar(UUID ordemDeServicoId, UUID pecaId, int quantidade);

    /**
     * Efetiva a baixa das reservas ativas da OS ao iniciar a execucao e devolve quantas
     * foram baixadas. Reservas ja baixadas sao ignoradas, entao repetir a chamada nao
     * desconta o estoque duas vezes.
     */
    int baixarReservasDaOrdemDeServico(UUID ordemDeServicoId);
}
