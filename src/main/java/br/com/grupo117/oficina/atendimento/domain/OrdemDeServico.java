package br.com.grupo117.oficina.atendimento.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

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

    /** Aplica somente as transições consecutivas previstas no Event Storming. */
    public void transicionarPara(StatusOS destino) {
        transicionarPara(destino, Instant.now());
    }

    public void transicionarPara(StatusOS destino, Instant momento) {
        Objects.requireNonNull(momento, "Momento da transição é obrigatório");
        Objects.requireNonNull(destino, "Status de destino é obrigatório");
        boolean permitida = switch (status) {
            case RECEBIDA -> destino == StatusOS.EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> destino == StatusOS.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> destino == StatusOS.EM_EXECUCAO;
            case EM_EXECUCAO -> destino == StatusOS.FINALIZADA;
            case FINALIZADA -> destino == StatusOS.ENTREGUE;
            case ENTREGUE -> false;
        };
        if (!permitida) {
            throw new IllegalStateException("Transição de " + status + " para " + destino + " não permitida");
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
