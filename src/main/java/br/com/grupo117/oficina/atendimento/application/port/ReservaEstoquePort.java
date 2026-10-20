package br.com.grupo117.oficina.atendimento.application.port;

import java.util.UUID;

public interface ReservaEstoquePort {
    /**
     * Reserva atomicamente apenas se houver disponibilidade, sem efetuar baixa.
     * itemId identifica a operação; repetições não devem reservar duas vezes.
     * Retorna false sem efeitos quando não há quantidade disponível.
     */
    boolean reservar(UUID osId, UUID itemId, UUID pecaId, int quantidade);
}
