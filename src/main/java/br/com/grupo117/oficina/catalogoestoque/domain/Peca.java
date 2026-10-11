package br.com.grupo117.oficina.catalogoestoque.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Peca ou insumo controlado no estoque. Separa a quantidade disponivel (fisica)
 * da reservada para ordens de servico: a reserva so diminui o que esta livre, e a
 * peca sai do estoque apenas na baixa, ao iniciar a execucao da OS.
 */
public final class Peca {

    private final UUID id;
    private String nome;
    private String descricao;
    private TipoItemEstoque tipo;
    private int quantidadeDisponivel;
    private int quantidadeReservada;
    private final List<EventoDeDominio> eventos = new ArrayList<>();

    private Peca(
            UUID id,
            String nome,
            String descricao,
            TipoItemEstoque tipo,
            int quantidadeDisponivel,
            int quantidadeReservada
    ) {
        this.id = Objects.requireNonNull(id, "Id da peca e obrigatorio");
        this.nome = nomeObrigatorio(nome);
        this.descricao = textoOpcional(descricao);
        this.tipo = Objects.requireNonNull(tipo, "Tipo do item de estoque e obrigatorio");
        if (quantidadeDisponivel < 0 || quantidadeReservada < 0) {
            throw new QuantidadeInvalidaException("Quantidade em estoque nao pode ser negativa");
        }
        if (quantidadeReservada > quantidadeDisponivel) {
            throw new IllegalArgumentException("Quantidade reservada nao pode exceder a disponivel");
        }
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.quantidadeReservada = quantidadeReservada;
    }

    public static Peca cadastrar(String nome, String descricao, TipoItemEstoque tipo, int quantidadeInicial) {
        Peca peca = new Peca(UUID.randomUUID(), nome, descricao, tipo, quantidadeInicial, 0);
        peca.eventos.add(new PecaAdicionada(peca.id, peca.nome, Instant.now()));
        return peca;
    }

    public static Peca reconstituir(
            UUID id,
            String nome,
            String descricao,
            TipoItemEstoque tipo,
            int quantidadeDisponivel,
            int quantidadeReservada
    ) {
        return new Peca(id, nome, descricao, tipo, quantidadeDisponivel, quantidadeReservada);
    }

    public void alterarDados(String nome, String descricao, TipoItemEstoque tipo) {
        String nomeValido = nomeObrigatorio(nome);
        this.tipo = Objects.requireNonNull(tipo, "Tipo do item de estoque e obrigatorio");
        this.nome = nomeValido;
        this.descricao = textoOpcional(descricao);
    }

    public void reabastecer(int quantidade) {
        if (quantidade <= 0) {
            throw new QuantidadeInvalidaException("Quantidade de entrada deve ser maior que zero");
        }
        quantidadeDisponivel += quantidade;
        eventos.add(new EstoqueReabastecido(id, quantidade, Instant.now()));
    }

    public ReservaDeEstoque reservar(UUID ordemDeServicoId, int quantidade) {
        QuantidadeReservada solicitada = QuantidadeReservada.de(quantidade);
        int livre = quantidadeLivre();
        if (solicitada.valor() > livre) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente: solicitado " + solicitada.valor() + ", livre " + livre);
        }
        ReservaDeEstoque reserva = ReservaDeEstoque.criar(id, ordemDeServicoId, solicitada);
        quantidadeReservada += solicitada.valor();
        eventos.add(new PecaReservada(id, reserva.id(), ordemDeServicoId, solicitada.valor(), Instant.now()));
        return reserva;
    }

    public void baixar(ReservaDeEstoque reserva) {
        exigirReservaDestaPeca(reserva);
        reserva.marcarBaixada();
        int quantidade = reserva.quantidade().valor();
        quantidadeDisponivel -= quantidade;
        quantidadeReservada -= quantidade;
        eventos.add(new EstoqueBaixado(id, reserva.id(), reserva.ordemDeServicoId(), quantidade, Instant.now()));
    }

    public void cancelarReserva(ReservaDeEstoque reserva) {
        exigirReservaDestaPeca(reserva);
        reserva.marcarCancelada();
        int quantidade = reserva.quantidade().valor();
        quantidadeReservada -= quantidade;
        eventos.add(new ReservaCancelada(id, reserva.id(), reserva.ordemDeServicoId(), quantidade, Instant.now()));
    }

    public int quantidadeLivre() {
        return quantidadeDisponivel - quantidadeReservada;
    }

    public List<EventoDeDominio> eventos() {
        return List.copyOf(eventos);
    }

    public void limparEventos() {
        eventos.clear();
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public TipoItemEstoque tipo() {
        return tipo;
    }

    public int quantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public int quantidadeReservada() {
        return quantidadeReservada;
    }

    private void exigirReservaDestaPeca(ReservaDeEstoque reserva) {
        Objects.requireNonNull(reserva, "Reserva e obrigatoria");
        if (!id.equals(reserva.pecaId())) {
            throw new IllegalArgumentException("Reserva " + reserva.id() + " nao pertence a peca " + id);
        }
    }

    private static String nomeObrigatorio(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da peca e obrigatorio");
        }
        return nome.trim();
    }

    private static String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Peca peca)) {
            return false;
        }
        return id.equals(peca.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Peca[id=" + id + ", nome=" + nome + ", tipo=" + tipo + ", disponivel=" + quantidadeDisponivel
                + ", reservada=" + quantidadeReservada + "]";
    }
}
