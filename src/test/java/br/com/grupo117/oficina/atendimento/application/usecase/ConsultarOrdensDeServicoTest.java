package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.List;
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
