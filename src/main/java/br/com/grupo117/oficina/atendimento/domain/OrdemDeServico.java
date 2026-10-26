package br.com.grupo117.oficina.atendimento.domain;

import java.time.Instant;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Orcamento;
import br.com.grupo117.oficina.atendimento.domain.valueobject.RejeicaoOrcamento;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Agregado responsável pelo ciclo de vida da OS.
 * Cliente e veículo são referenciados por seus identificadores (documento e placa).
 * A identificação e validação desses cadastros cabe ao caso de uso de criação.
 */
public final class OrdemDeServico {

    private final UUID id;
    private final String clienteId;
    private final String veiculoId;
    private StatusOS status;
    private Instant finalizadaEm;
    private Orcamento orcamento;
    private Instant iniciadaEm;
    private RejeicaoOrcamento rejeicaoOrcamento;
    private final List<ItemDaOS> itens = new ArrayList<>();

    public OrdemDeServico(UUID id, String clienteId, String veiculoId) {
        this.id = Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        this.clienteId = validarIdentificador(clienteId, "Cliente");
        this.veiculoId = validarIdentificador(veiculoId, "Veículo");
        this.status = StatusOS.RECEBIDA;
    }

    /** Reconstitui o estado persistido sem executar ações ou transições de negócio. */
    public static OrdemDeServico restaurar(UUID id, String clienteId, String veiculoId,
                                           StatusOS status, Instant finalizadaEm) {
        Objects.requireNonNull(status, "Status é obrigatório");
        boolean concluida = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE;
        if (concluida != (finalizadaEm != null)) {
            throw new IllegalArgumentException("Finalização incompatível com o status da OS");
        }
        OrdemDeServico os = new OrdemDeServico(id, clienteId, veiculoId);
        os.status = status;
        os.finalizadaEm = finalizadaEm;
        return os;
    }

    public static OrdemDeServico restaurar(UUID id, String clienteId, String veiculoId,
                                           StatusOS status, Instant finalizadaEm,
                                           List<ItemDaOS> itens, Orcamento orcamento) {
        OrdemDeServico os = restaurar(id, clienteId, veiculoId, status, finalizadaEm);
        List<ItemDaOS> copia = List.copyOf(itens);
        if (copia.stream().map(ItemDaOS::id).distinct().count() != copia.size()) {
            throw new IllegalArgumentException("Identificador de item duplicado na OS persistida");
        }
        if (orcamento != null && (status == StatusOS.RECEBIDA || status == StatusOS.EM_DIAGNOSTICO
                || !orcamento.itens().equals(copia))) {
            throw new IllegalArgumentException("Orçamento incompatível com o estado persistido da OS");
        }
        os.itens.addAll(copia);
        os.orcamento = orcamento;
        return os;
    }

    /** Restaura decisões já persistidas, sem aprovar, rejeitar ou movimentar estoque. */
    public static OrdemDeServico restaurar(UUID id, String clienteId, String veiculoId,
                                           StatusOS status, Instant finalizadaEm,
                                           List<ItemDaOS> itens, Orcamento orcamento,
                                           Instant iniciadaEm, RejeicaoOrcamento rejeicaoOrcamento) {
        OrdemDeServico os = restaurar(id, clienteId, veiculoId, status, finalizadaEm, itens, orcamento);
        boolean emExecucaoOuConcluida = status == StatusOS.EM_EXECUCAO
                || status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE;
        if (iniciadaEm != null && (!emExecucaoOuConcluida || orcamento == null)) {
            throw new IllegalArgumentException("Início incompatível com o estado persistido da OS");
        }
        if (iniciadaEm != null && finalizadaEm != null && finalizadaEm.isBefore(iniciadaEm)) {
            throw new IllegalArgumentException("Finalização não pode anteceder o início da execução");
        }
        if (rejeicaoOrcamento != null && (status != StatusOS.AGUARDANDO_APROVACAO
                || orcamento == null || iniciadaEm != null)) {
            throw new IllegalArgumentException("Rejeição incompatível com o estado persistido da OS");
        }
        os.iniciadaEm = iniciadaEm;
        os.rejeicaoOrcamento = rejeicaoOrcamento;
        return os;
    }

    public UUID id() {
        return id;
    }

    public String clienteId() {
        return clienteId;
    }

    public String veiculoId() {
        return veiculoId;
    }

    public StatusOS status() {
        return status;
    }

    public Optional<Instant> finalizadaEm() {
        return Optional.ofNullable(finalizadaEm);
    }

    public Optional<Orcamento> orcamento() { return Optional.ofNullable(orcamento); }

    public Optional<Instant> iniciadaEm() { return Optional.ofNullable(iniciadaEm); }

    public Optional<RejeicaoOrcamento> rejeicaoOrcamento() {
        return Optional.ofNullable(rejeicaoOrcamento);
    }

    public void validarAprovacaoOrcamento() {
        validarDecisaoOrcamento();
    }

    public void aprovarOrcamento(Instant momento) {
        Objects.requireNonNull(momento, "Momento da aprovação é obrigatório");
        validarAprovacaoOrcamento();
        transicionarPara(StatusOS.EM_EXECUCAO, momento);
        iniciadaEm = momento;
    }

    public void rejeitarOrcamento(Instant momento, String motivo) {
        validarDecisaoOrcamento();
        RejeicaoOrcamento rejeicao = new RejeicaoOrcamento(momento, motivo);
        rejeicaoOrcamento = rejeicao;
    }

    private void validarDecisaoOrcamento() {
        if (status != StatusOS.AGUARDANDO_APROVACAO || orcamento == null) {
            throw new IllegalStateException("Decisão exige orçamento gerado em Aguardando aprovação");
        }
        if (rejeicaoOrcamento != null) {
            throw new IllegalStateException("Orçamento rejeitado exige revisão antes de nova decisão");
        }
    }

    /** Política ao concluir o diagnóstico com itens: calcula o orçamento e envia para aprovação. */
    public Orcamento gerarOrcamento() {
        if (status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Geração de orçamento não permitida em " + status);
        }
        return calcularOrcamentoEEnviarParaAprovacao();
    }

    /** Submissão do mecânico, direta da OS recebida ou depois do diagnóstico. */
    public Orcamento submeter() {
        if (status != StatusOS.RECEBIDA && status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Submissão não permitida em " + status);
        }
        return calcularOrcamentoEEnviarParaAprovacao();
    }

    private Orcamento calcularOrcamentoEEnviarParaAprovacao() {
        Orcamento gerado = new Orcamento(itens);
        transicionarPara(StatusOS.AGUARDANDO_APROVACAO);
        orcamento = gerado;
        return gerado;
    }

    public List<ItemDaOS> itens() { return List.copyOf(itens); }

    public void validarInclusaoDeItem() {
        if (status != StatusOS.RECEBIDA && status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Inclusão de itens não permitida em " + status);
        }
    }

    public void incluirItem(ItemDaOS item) {
        Objects.requireNonNull(item, "Item é obrigatório");
        validarInclusaoDeItem();
        if (itens.stream().anyMatch(existente -> existente.id().equals(item.id()))) {
            throw new IllegalArgumentException("Identificador de item já incluído na OS");
        }
        itens.add(item);
    }

    /** Aplica as transições previstas no Event Storming, inclusive a submissão direta da OS recebida. */
    public void transicionarPara(StatusOS destino) {
        transicionarPara(destino, Instant.now());
    }

    public void transicionarPara(StatusOS destino, Instant momento) {
        Objects.requireNonNull(momento, "Momento da transição é obrigatório");
        Objects.requireNonNull(destino, "Status de destino é obrigatório");
        boolean permitida = switch (status) {
            case RECEBIDA -> destino == StatusOS.EM_DIAGNOSTICO || destino == StatusOS.AGUARDANDO_APROVACAO;
            case EM_DIAGNOSTICO -> destino == StatusOS.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> destino == StatusOS.EM_EXECUCAO;
            case EM_EXECUCAO -> destino == StatusOS.FINALIZADA;
            case FINALIZADA -> destino == StatusOS.ENTREGUE;
            case ENTREGUE -> false;
        };
        if (!permitida) {
            throw new IllegalStateException("Transição de " + status + " para " + destino + " não permitida");
        }
        if (destino == StatusOS.EM_EXECUCAO && rejeicaoOrcamento != null) {
            throw new IllegalStateException("Orçamento rejeitado não pode iniciar execução");
        }
        if (destino == StatusOS.FINALIZADA && iniciadaEm != null && momento.isBefore(iniciadaEm)) {
            throw new IllegalArgumentException("Finalização não pode anteceder o início da execução");
        }
        status = destino;
        if (destino == StatusOS.FINALIZADA) {
            finalizadaEm = momento;
        }
    }

    private static String validarIdentificador(String valor, String nome) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nome + " é obrigatório");
        }
        return valor;
    }
}
