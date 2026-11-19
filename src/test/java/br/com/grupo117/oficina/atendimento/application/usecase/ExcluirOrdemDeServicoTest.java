package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ExcluirOrdemDeServicoTest {
    @Test
    void liberaReservasAntesDeExcluir() {
        var os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        List<String> operacoes = new ArrayList<>();
        var caso = new ExcluirOrdemDeServico(new RepositorioFake(os),
                id -> { assertThat(id).isEqualTo(os.id()); operacoes.add("reservas"); },
                id -> { assertThat(id).isEqualTo(os.id()); operacoes.add("exclusao"); });

        caso.excluir(os.id());

        assertThat(operacoes).containsExactly("reservas", "exclusao");
    }

    @Test
    void osAusenteOuIdentificadorNuloNaoMovimentaEstoque() {
        var caso = new ExcluirOrdemDeServico(new RepositorioFake(null),
                id -> fail("Não deve cancelar reservas"), id -> fail("Não deve excluir"));

        assertThatThrownBy(() -> caso.excluir(UUID.randomUUID()))
                .isInstanceOf(OrdemDeServicoNaoEncontradaException.class);
        assertThatNullPointerException().isThrownBy(() -> caso.excluir(null));
    }

    @Test
    void falhaAoLiberarReservasImpedeExclusao() {
        var os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        var caso = new ExcluirOrdemDeServico(new RepositorioFake(os),
                id -> { throw new IllegalStateException("Falha no estoque"); },
                id -> fail("Não deve excluir"));

        assertThatIllegalStateException().isThrownBy(() -> caso.excluir(os.id()))
                .withMessage("Falha no estoque");
    }

    private record RepositorioFake(OrdemDeServico os) implements OrdemDeServicoRepositorio {
        @Override
        public Optional<OrdemDeServico> buscarPorId(UUID id) {
            return Optional.ofNullable(os).filter(encontrada -> encontrada.id().equals(id));
        }

        @Override
        public List<OrdemDeServico> listar() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrdemDeServico salvar(OrdemDeServico ordem) {
            throw new UnsupportedOperationException();
        }
    }
}
