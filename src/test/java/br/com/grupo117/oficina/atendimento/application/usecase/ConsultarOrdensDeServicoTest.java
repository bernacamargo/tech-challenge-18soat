package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.List;
import java.time.Instant;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ConsultarOrdensDeServicoTest {
    @Test
    void listaEDetalhaOrdensExistentes() {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        OrdemDeServicoRepositorio repo = new ConsultaFake(List.of(os));
        assertThat(new ListarOrdensDeServico(repo).listar()).containsExactly(os);
        assertThat(new DetalharOrdemDeServico(repo).detalhar(os.id())).isSameAs(os);
    }

    @Test
    void listaVaziaEOsAusente() {
        OrdemDeServicoRepositorio repo = new ConsultaFake(List.of());
        DetalharOrdemDeServico consulta = new DetalharOrdemDeServico(repo);
        UUID id = UUID.randomUUID();
        assertThat(new ListarOrdensDeServico(repo).listar()).isEmpty();
        assertThatThrownBy(() -> consulta.detalhar(id)).isInstanceOf(OrdemDeServicoNaoEncontradaException.class);
        assertThatNullPointerException().isThrownBy(() -> consulta.detalhar(null));
    }

    @Test
    void priorizaStatusAntesDaAntiguidadeEOmiteConcluidas() {
        Instant antiga = Instant.parse("2026-11-01T00:00:00Z");
        Instant recente = antiga.plusSeconds(3600);
        OrdemDeServico recebida = restaurar(1, StatusOS.RECEBIDA, antiga);
        OrdemDeServico diagnostico = restaurar(2, StatusOS.EM_DIAGNOSTICO, antiga);
        OrdemDeServico aguardando = restaurar(3, StatusOS.AGUARDANDO_APROVACAO, antiga);
        OrdemDeServico execucaoAntiga = restaurar(4, StatusOS.EM_EXECUCAO, antiga);
        OrdemDeServico execucaoRecente = restaurar(5, StatusOS.EM_EXECUCAO, recente);
        OrdemDeServico finalizada = restaurar(6, StatusOS.FINALIZADA, antiga);
        OrdemDeServico entregue = restaurar(7, StatusOS.ENTREGUE, antiga);
        var repo = new ConsultaFake(List.of(recebida, entregue, diagnostico,
                execucaoRecente, finalizada, aguardando, execucaoAntiga));

        assertThat(new ListarOrdensDeServico(repo).listar()).containsExactly(
                execucaoAntiga, execucaoRecente, aguardando, diagnostico, recebida);
        assertThat(new DetalharOrdemDeServico(repo).detalhar(finalizada.id())).isSameAs(finalizada);
        assertThat(new DetalharOrdemDeServico(repo).detalhar(entregue.id())).isSameAs(entregue);
    }

    @Test
    void ordenaLegadasPrimeiroEDesempataDatasPorUuid() {
        Instant momento = Instant.parse("2026-11-01T00:00:00Z");
        OrdemDeServico legadaMenor = restaurar(1, StatusOS.RECEBIDA, null);
        OrdemDeServico legadaMaior = restaurar(2, StatusOS.RECEBIDA, null);
        OrdemDeServico datadaMenor = restaurar(3, StatusOS.RECEBIDA, momento);
        OrdemDeServico datadaMaior = restaurar(4, StatusOS.RECEBIDA, momento);
        var repo = new ConsultaFake(List.of(datadaMaior, legadaMaior, datadaMenor, legadaMenor));

        assertThat(new ListarOrdensDeServico(repo).listar())
                .containsExactly(legadaMenor, legadaMaior, datadaMenor, datadaMaior);
    }

    private static OrdemDeServico restaurar(long id, StatusOS status, Instant abertura) {
        Instant fim = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE
                ? Instant.parse("2026-11-02T00:00:00Z") : null;
        return OrdemDeServico.restaurar(new UUID(0, id), "52998224725", "ABC1D23",
                status, fim, List.of(), null, null, null, abertura);
    }

    private record ConsultaFake(List<OrdemDeServico> ordens) implements OrdemDeServicoRepositorio {
        @Override
        public Optional<OrdemDeServico> buscarPorId(UUID id) {
            return ordens.stream().filter(os -> os.id().equals(id)).findFirst();
        }
        @Override
        public List<OrdemDeServico> listar() { return ordens; }
        @Override
        public OrdemDeServico salvar(OrdemDeServico os) { throw new UnsupportedOperationException(); }
    }
}
