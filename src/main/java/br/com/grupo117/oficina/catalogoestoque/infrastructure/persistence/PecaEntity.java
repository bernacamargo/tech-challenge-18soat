package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "peca")
public class PecaEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 150)
    private String nome;
    @Column(length = 500)
    private String descricao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItemEstoque tipo;
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;
    @Column(name = "quantidade_disponivel", nullable = false)
    private int quantidadeDisponivel;
    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;
    @Version
    private Long versao;

    protected PecaEntity() {}

    static PecaEntity nova(UUID id) {
        PecaEntity entity = new PecaEntity();
        entity.id = id;
        return entity;
    }

    /** Copia o estado do agregado preservando a versao, para o lock otimista detectar conflitos. */
    void atualizar(Peca peca) {
        nome = peca.nome();
        descricao = peca.descricao();
        tipo = peca.tipo();
        precoUnitario = peca.precoUnitario().valor();
        quantidadeDisponivel = peca.quantidadeDisponivel();
        quantidadeReservada = peca.quantidadeReservada();
    }

    Peca paraDominio() {
        return Peca.restaurar(id, nome, descricao, tipo, Dinheiro.de(precoUnitario),
                quantidadeDisponivel, quantidadeReservada);
    }
}
