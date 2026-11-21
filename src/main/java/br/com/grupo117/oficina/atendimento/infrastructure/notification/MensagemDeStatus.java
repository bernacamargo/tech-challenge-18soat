package br.com.grupo117.oficina.atendimento.infrastructure.notification;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.text.NumberFormat;
import java.util.Locale;

/** Texto simples da situação da OS. Sem CPF, credenciais ou link de decisão. */
public final class MensagemDeStatus {
    /** Chave de idempotência da rejeição; o status da OS permanece Aguardando aprovação. */
    public static final String REJEICAO = "ORCAMENTO_REJEITADO";

    private MensagemDeStatus() { }

    public record Mensagem(String situacao, String assunto, String corpo) { }

    public static Mensagem de(OrdemDeServico os) {
        if (os.status() == StatusOS.AGUARDANDO_APROVACAO && os.rejeicaoOrcamento().isPresent()) {
            return rejeicao(os);
        }
        return switch (os.status()) {
            case RECEBIDA -> texto(os, "OS recebida ",
                    "Recebemos o seu veículo e abrimos a ordem de serviço.", "Recebida");
            case EM_DIAGNOSTICO -> texto(os, "OS em diagnóstico ",
                    "O diagnóstico do seu veículo foi iniciado.", "Em diagnóstico");
            case AGUARDANDO_APROVACAO -> orcamento(os);
            case EM_EXECUCAO -> texto(os, "OS em execução ",
                    "Seu orçamento foi aprovado e a execução dos serviços começou.", "Em execução");
            case FINALIZADA -> texto(os, "OS finalizada ",
                    "Os serviços da sua ordem foram concluídos.", "Finalizada");
            case ENTREGUE -> texto(os, "OS entregue ",
                    "O veículo foi entregue e a ordem de serviço está encerrada.", "Entregue");
        };
    }

    private static Mensagem texto(OrdemDeServico os, String assunto, String introducao, String situacaoLegivel) {
        String corpo = introducao + "\n\nOS: " + os.id() + "\nVeículo: " + os.veiculoId()
                + "\nSituação: " + situacaoLegivel;
        return new Mensagem(os.status().name(), assunto + os.id(), corpo);
    }

    private static Mensagem orcamento(OrdemDeServico os) {
        var moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        var corpo = new StringBuilder("Seu orçamento está pronto para aprovação.\n\nOS: ")
                .append(os.id()).append("\nVeículo: ").append(os.veiculoId()).append("\n\n");
        os.itens().forEach(item -> corpo.append(item.descricao()).append(" — ")
                .append(item.quantidade()).append(" x ").append(moeda.format(item.precoUnitario().valor()))
                .append(" = ").append(moeda.format(item.subtotal().valor())).append('\n'));
        corpo.append("\nTotal: ").append(moeda.format(os.orcamento().orElseThrow().total().valor()))
                .append("\n\nInforme sua aprovação ou rejeição à oficina, identificando a OS acima.");
        return new Mensagem(StatusOS.AGUARDANDO_APROVACAO.name(), "Orçamento da OS " + os.id(), corpo.toString());
    }

    private static Mensagem rejeicao(OrdemDeServico os) {
        String motivo = os.rejeicaoOrcamento().orElseThrow().motivo();
        String corpo = "O orçamento da sua ordem de serviço foi rejeitado.\n\nOS: " + os.id()
                + "\nVeículo: " + os.veiculoId() + "\nMotivo: " + motivo;
        return new Mensagem(REJEICAO, "Orçamento rejeitado da OS " + os.id(), corpo);
    }
}
