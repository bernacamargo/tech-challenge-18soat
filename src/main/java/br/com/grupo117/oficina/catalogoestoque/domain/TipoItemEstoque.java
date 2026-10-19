package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Natureza do item controlado no estoque: peca de reposicao ou insumo consumido
 * no servico (ex.: oleo, fluido). Ambos sao controlados em unidades.
 */
public enum TipoItemEstoque {
    PECA,
    INSUMO
}
