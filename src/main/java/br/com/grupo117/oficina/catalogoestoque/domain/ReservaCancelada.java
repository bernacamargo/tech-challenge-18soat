package br.com.grupo117.oficina.catalogoestoque.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Reserva desfeita: a quantidade volta a ficar livre para outras OS.
 */
public record ReservaCancelada(
        UUID pecaId,
        UUID reservaId,
        UUID ordemDeServicoId,
        int quantidade,
        Instant ocorridoEm
) implements EventoDeDominio {
}
