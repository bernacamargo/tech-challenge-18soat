package br.com.grupo117.oficina.atendimento.application.port;

import java.util.UUID;

/** Contrato de consumo das reservas, sem entidades de Catálogo/Estoque. */
public interface BaixaEstoquePort {
    /**
     * Consome integralmente as reservas da OS ou lança exceção sem efeitos parciais.
     * Deve ser idempotente por OS e integrar a mesma transação da gravação da OS.
     * O adaptador deve verificar a correspondência com os itens; ausência inesperada
     * de reservas de peças não equivale a sucesso. OS só com serviços não exige reserva.
     */
    void baixarReservas(UUID osId);
}
