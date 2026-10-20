package br.com.grupo117.oficina.catalogoestoque.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Quantidade da peca separada para uma ordem de servico.
 */
public record PecaReservada(
        UUID pecaId,
        UUID reservaId,
        UUID ordemDeServicoId,
        int quantidade,
        Instant ocorridoEm
) implements EventoDeDominio {
}
