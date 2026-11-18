package br.com.grupo117.oficina.atendimento.application.port;

import java.util.UUID;

public interface CancelamentoReservasOSPort {
    /** Libera somente reservas ainda ativas da OS. */
    void cancelarReservasAtivas(UUID osId);
}
