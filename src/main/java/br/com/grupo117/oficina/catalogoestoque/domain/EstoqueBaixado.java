package br.com.grupo117.oficina.catalogoestoque.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Reserva consumida no inicio da execucao da OS: a quantidade sai do estoque.
 */
public record EstoqueBaixado(
        UUID pecaId,
        UUID reservaId,
        UUID ordemDeServicoId,
        int quantidade,
        Instant ocorridoEm
) implements EventoDeDominio {
}
