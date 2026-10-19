package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Ciclo de vida da reserva: nasce ativa e termina baixada (consumida no inicio
 * da execucao da OS) ou cancelada (quantidade devolvida ao estoque livre).
 */
public enum StatusReserva {
    ATIVA,
    BAIXADA,
    CANCELADA
}
