package br.com.grupo117.oficina.catalogoestoque.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Peca ou insumo cadastrado no catalogo de estoque.
 */
public record PecaAdicionada(UUID pecaId, String nome, Instant ocorridoEm) implements EventoDeDominio {
}
