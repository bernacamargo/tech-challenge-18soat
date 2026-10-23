package br.com.grupo117.oficina.atendimento.application.port;

import java.util.UUID;

/** Devolve ao saldo livre as reservas ainda ativas quando a OS é rejeitada. */
public interface DevolucaoEstoquePort {
    /**
     * Cancela as reservas ativas da OS. OS só com serviços não tem reserva e encerra sem efeito.
     * Deve participar da mesma transação da gravação da rejeição.
     */
    void devolverReservas(UUID osId);
}
