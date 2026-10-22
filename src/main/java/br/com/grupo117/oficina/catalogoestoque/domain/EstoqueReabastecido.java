package br.com.grupo117.oficina.catalogoestoque.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrada de uma quantidade no estoque disponivel da peca.
 */
public record EstoqueReabastecido(UUID pecaId, int quantidade, Instant ocorridoEm) implements EventoDeDominio {
}
