package br.com.grupo117.oficina.atendimento.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/** Projeção de integração; não expõe entidades do contexto de catálogo. */
public record ItemCatalogo(UUID id, String descricao, BigDecimal preco) {}
