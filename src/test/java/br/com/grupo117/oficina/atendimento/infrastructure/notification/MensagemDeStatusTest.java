package br.com.grupo117.oficina.atendimento.infrastructure.notification;

import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

class MensagemDeStatusTest {
    private static final Instant FIM = Instant.parse("2026-11-18T12:00:00Z");

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = "AGUARDANDO_APROVACAO", mode = EnumSource.Mode.EXCLUDE)
    void cadaStatusTemTextoProprioSemDocumentoNemLink(StatusOS status) {
        UUID id = UUID.randomUUID();
        Instant fim = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE ? FIM : null;
        OrdemDeServico os = OrdemDeServico.restaurar(id, "52998224725", "ABC1D23", status, fim);

        MensagemDeStatus.Mensagem mensagem = MensagemDeStatus.de(os);

        assertThat(mensagem.situacao()).isEqualTo(status.name());
        assertThat(mensagem.assunto()).contains(id.toString()).doesNotContain("52998224725");
        assertThat(mensagem.corpo()).contains(id.toString(), "ABC1D23")
                .doesNotContain("52998224725", "http");
        assertThat(mensagem.corpo()).contains(trecho(status));
    }

    @Test
    void orcamentoListaItensETotalSemLinkDeDecisao() {
        UUID id = UUID.randomUUID();
        ItemDaOS servico = new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Troca de óleo", 1, new Dinheiro(new BigDecimal("80.50")));
        ItemDaOS peca = new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.PECA,
                "Óleo", 3, new Dinheiro(new BigDecimal("25.25")));
        OrdemDeServico os = new OrdemDeServico(id, "52998224725", "ABC1D23");
        os.incluirItem(servico);
        os.incluirItem(peca);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();

        MensagemDeStatus.Mensagem mensagem = MensagemDeStatus.de(os);

        assertThat(mensagem.situacao()).isEqualTo("AGUARDANDO_APROVACAO");
        assertThat(mensagem.assunto()).isEqualTo("Orçamento da OS " + id);
        assertThat(mensagem.corpo()).contains("pronto para aprovação", "Troca de óleo", "Óleo",
                "80,50", "25,25", "156,25", "ABC1D23", id.toString());
        assertThat(mensagem.corpo()).doesNotContain("http", "52998224725");
    }

    @Test
    void rejeicaoUsaChavePropriaEIncluiOMotivo() {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Revisão", 1, new Dinheiro(new BigDecimal("80.50"))));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        os.rejeitarOrcamento(FIM, "  Preço acima do esperado  ");

        MensagemDeStatus.Mensagem mensagem = MensagemDeStatus.de(os);

        assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(mensagem.situacao()).isEqualTo(MensagemDeStatus.REJEICAO);
        assertThat(mensagem.assunto()).isEqualTo("Orçamento rejeitado da OS " + os.id());
        assertThat(mensagem.corpo()).contains("foi rejeitado", "Preço acima do esperado", "ABC1D23")
                .doesNotContain("http", "52998224725");
    }

    private static String trecho(StatusOS status) {
        return switch (status) {
            case RECEBIDA -> "Recebemos o seu veículo";
            case EM_DIAGNOSTICO -> "diagnóstico do seu veículo foi iniciado";
            case EM_EXECUCAO -> "execução dos serviços começou";
            case FINALIZADA -> "foram concluídos";
            case ENTREGUE -> "veículo foi entregue";
            case AGUARDANDO_APROVACAO -> throw new IllegalArgumentException(status.name());
        };
    }
}
