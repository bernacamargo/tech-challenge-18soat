package br.com.grupo117.oficina.catalogoestoque.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.grupo117.oficina.catalogoestoque.domain.DinheiroInvalidoException;
import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicoCrudTest {

    private ServicoRepositorioFake repositorio;
    private CadastrarServico cadastrar;
    private ListarServicos listar;
    private DetalharServico detalhar;
    private AlterarServico alterar;
    private RemoverServico remover;

    @BeforeEach
    void setUp() {
        repositorio = new ServicoRepositorioFake();
        cadastrar = new CadastrarServico(repositorio);
        listar = new ListarServicos(repositorio);
        detalhar = new DetalharServico(repositorio);
        alterar = new AlterarServico(repositorio);
        remover = new RemoverServico(repositorio);
    }

    @Test
    void cadastraListaDetalhaAlteraERemove() {
        Servico troca = cadastrar.cadastrar("  Troca de oleo  ", "  ", new BigDecimal("150"));

        assertEquals("Troca de oleo", troca.nome());
        assertNull(troca.descricao());
        assertEquals(new BigDecimal("150.00"), detalhar.detalhar(troca.id()).preco().valor());
        assertEquals(List.of(troca), listar.listar());

        Servico alterado = alterar.alterar(troca.id(), "Troca de oleo sintetico", "Inclui filtro", new BigDecimal("220"));
        assertEquals(troca.id(), alterado.id());
        assertEquals("Troca de oleo sintetico", detalhar.detalhar(troca.id()).nome());
        assertEquals("Inclui filtro", detalhar.detalhar(troca.id()).descricao());
        assertEquals(new BigDecimal("220.00"), detalhar.detalhar(troca.id()).preco().valor());

        remover.remover(troca.id());
        assertTrue(listar.listar().isEmpty());
    }

    @Test
    void nomeRepetidoCadastraOutroServico() {
        cadastrar.cadastrar("Alinhamento", null, new BigDecimal("80"));
        cadastrar.cadastrar("Alinhamento", "Conferencia", new BigDecimal("90"));

        assertEquals(2, listar.listar().size());
    }

    @Test
    void precoInvalidoNaoAlteraORepositorio() {
        assertThrows(DinheiroInvalidoException.class, () -> cadastrar.cadastrar("Troca de oleo", null, null));
        assertThrows(DinheiroInvalidoException.class, () -> cadastrar.cadastrar("Troca de oleo", null, BigDecimal.ZERO));
        assertThrows(
                DinheiroInvalidoException.class,
                () -> cadastrar.cadastrar("Troca de oleo", null, new BigDecimal("-1"))
        );
        assertTrue(repositorio.vazio());
    }

    @Test
    void precoInvalidoNaAlteracaoMantemOServicoSalvo() {
        Servico troca = cadastrar.cadastrar("Troca de oleo", null, new BigDecimal("150"));

        assertThrows(
                DinheiroInvalidoException.class,
                () -> alterar.alterar(troca.id(), "Troca de oleo", null, BigDecimal.ZERO)
        );

        assertEquals(new BigDecimal("150.00"), detalhar.detalhar(troca.id()).preco().valor());
        assertEquals("Troca de oleo", detalhar.detalhar(troca.id()).nome());
    }

    @Test
    void servicoInexistenteNaoEDetalhadoAlteradoNemRemovido() {
        UUID id = UUID.randomUUID();

        assertThrows(ServicoNaoEncontradoException.class, () -> detalhar.detalhar(id));
        assertThrows(ServicoNaoEncontradoException.class, () -> alterar.alterar(id, "Troca", null, new BigDecimal("10")));
        assertThrows(ServicoNaoEncontradoException.class, () -> remover.remover(id));
        assertTrue(repositorio.vazio());
    }

    private static final class ServicoRepositorioFake implements ServicoRepositorio {

        private final Map<UUID, Servico> servicos = new HashMap<>();

        @Override
        public Optional<Servico> buscarPorId(UUID id) {
            return Optional.ofNullable(servicos.get(id));
        }

        @Override
        public List<Servico> listar() {
            return List.copyOf(servicos.values());
        }

        @Override
        public Servico salvar(Servico servico) {
            servicos.put(servico.id(), servico);
            return servico;
        }

        @Override
        public void remover(UUID id) {
            servicos.remove(id);
        }

        boolean vazio() {
            return servicos.isEmpty();
        }
    }
}
