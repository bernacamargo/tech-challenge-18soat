package br.com.grupo117.oficina.catalogoestoque.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Quantidade de uma peca separada para uma ordem de servico antes da execucao.
 * A OS e referenciada apenas pelo id, sem acoplar ao contexto de atendimento.
 * Muda de status somente pela Peca, que ajusta o estoque na mesma operacao.
 */
public final class ReservaDeEstoque {

    private final UUID id;
    private final UUID pecaId;
    private final UUID ordemDeServicoId;
    private final QuantidadeReservada quantidade;
    private StatusReserva status;

    private ReservaDeEstoque(
            UUID id,
            UUID pecaId,
            UUID ordemDeServicoId,
            QuantidadeReservada quantidade,
            StatusReserva status
    ) {
        this.id = Objects.requireNonNull(id, "Id da reserva e obrigatorio");
        this.pecaId = Objects.requireNonNull(pecaId, "Peca da reserva e obrigatoria");
        this.ordemDeServicoId = Objects.requireNonNull(ordemDeServicoId, "Ordem de servico da reserva e obrigatoria");
        this.quantidade = Objects.requireNonNull(quantidade, "Quantidade da reserva e obrigatoria");
        this.status = Objects.requireNonNull(status, "Status da reserva e obrigatorio");
    }

    static ReservaDeEstoque criar(UUID pecaId, UUID ordemDeServicoId, QuantidadeReservada quantidade) {
        return new ReservaDeEstoque(UUID.randomUUID(), pecaId, ordemDeServicoId, quantidade, StatusReserva.ATIVA);
    }

    public static ReservaDeEstoque reconstituir(
            UUID id,
            UUID pecaId,
            UUID ordemDeServicoId,
            int quantidade,
            StatusReserva status
    ) {
        return new ReservaDeEstoque(id, pecaId, ordemDeServicoId, QuantidadeReservada.de(quantidade), status);
    }

    void marcarBaixada() {
        exigirAtiva("baixada");
        status = StatusReserva.BAIXADA;
    }

    void marcarCancelada() {
        exigirAtiva("cancelada");
        status = StatusReserva.CANCELADA;
    }

    public boolean ativa() {
        return status == StatusReserva.ATIVA;
    }

    public UUID id() {
        return id;
    }

    public UUID pecaId() {
        return pecaId;
    }

    public UUID ordemDeServicoId() {
        return ordemDeServicoId;
    }

    public QuantidadeReservada quantidade() {
        return quantidade;
    }

    public StatusReserva status() {
        return status;
    }

    private void exigirAtiva(String operacao) {
        if (!ativa()) {
            throw new ReservaEncerradaException(
                    "Reserva " + id + " nao pode ser " + operacao + ": status " + status);
        }
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof ReservaDeEstoque reserva)) {
            return false;
        }
        return id.equals(reserva.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ReservaDeEstoque[id=" + id + ", pecaId=" + pecaId + ", ordemDeServicoId=" + ordemDeServicoId
                + ", quantidade=" + quantidade.valor() + ", status=" + status + "]";
    }
}
