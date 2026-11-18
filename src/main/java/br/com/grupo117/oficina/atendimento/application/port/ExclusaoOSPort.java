package br.com.grupo117.oficina.atendimento.application.port;

import java.util.UUID;

public interface ExclusaoOSPort {
    /** Marca a OS como excluída e cancela suas notificações pendentes. */
    void excluir(UUID osId);
}
