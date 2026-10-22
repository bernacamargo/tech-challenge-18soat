package br.com.grupo117.oficina.atendimento.application.port;

import java.util.Optional;
import java.util.UUID;

public interface CatalogoOSPort {
    Optional<ItemCatalogo> buscarServico(UUID id);
    Optional<ItemCatalogo> buscarPeca(UUID id);
}
