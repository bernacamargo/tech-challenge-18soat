package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.math.BigDecimal;
import java.util.UUID;

public record PecaResponse(UUID id, String nome, String descricao, TipoItemEstoque tipo, BigDecimal precoUnitario,
        int quantidadeDisponivel, int quantidadeReservada, int quantidadeLivre) {
    public static PecaResponse de(Peca peca) {
        return new PecaResponse(peca.id(), peca.nome(), peca.descricao(), peca.tipo(), peca.precoUnitario().valor(),
                peca.quantidadeDisponivel(), peca.quantidadeReservada(), peca.quantidadeLivre());
    }
}
