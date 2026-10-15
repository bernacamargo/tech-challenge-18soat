package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import br.com.grupo117.oficina.catalogoestoque.domain.ReservaDeEstoque;
import br.com.grupo117.oficina.catalogoestoque.domain.StatusReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "reserva_de_estoque")
public class ReservaDeEstoqueEntity {
    @Id
    private UUID id;
    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;
    @Column(name = "ordem_de_servico_id", nullable = false)
    private UUID ordemDeServicoId;
    @Column(nullable = false)
    private int quantidade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusReserva status;

    protected ReservaDeEstoqueEntity() {}

    static ReservaDeEstoqueEntity de(ReservaDeEstoque reserva) {
        ReservaDeEstoqueEntity entity = new ReservaDeEstoqueEntity();
        entity.id = reserva.id();
        entity.pecaId = reserva.pecaId();
        entity.ordemDeServicoId = reserva.ordemDeServicoId();
        entity.quantidade = reserva.quantidade().valor();
        entity.status = reserva.status();
        return entity;
    }

    ReservaDeEstoque paraDominio() {
        return ReservaDeEstoque.restaurar(id, pecaId, ordemDeServicoId, quantidade, status);
    }
}
