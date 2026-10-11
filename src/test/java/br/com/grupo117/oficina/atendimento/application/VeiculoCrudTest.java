package br.com.grupo117.oficina.atendimento.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.PlacaInvalidaException;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VeiculoCrudTest {

    private final Cliente maria = Cliente.cadastrar("Maria Souza", "52998224725");
    private VeiculoRepositorioFake repositorio;
    private ListarVeiculos listar;
    private DetalharVeiculo detalhar;
    private AlterarVeiculo alterar;
    private RemoverVeiculo remover;

    @BeforeEach
    void setUp() {
        repositorio = new VeiculoRepositorioFake();
        repositorio.salvar(Veiculo.cadastrar(maria, "ABC1D23", "Fiat", "Uno", 2012));
        listar = new ListarVeiculos(repositorio);
        detalhar = new DetalharVeiculo(repositorio);
        alterar = new AlterarVeiculo(repositorio);
        remover = new RemoverVeiculo(repositorio);
    }

    @Test
    void listaDetalhaAlteraERemoveMantendoOCliente() {
        assertEquals("ABC1D23", detalhar.detalhar("abc-1d23").placa().valor());
        assertEquals(maria, detalhar.detalhar("ABC1D23").cliente());
        assertEquals(1, listar.listar().size());

        Veiculo alterado = alterar.alterar("abc-1d23", " Fiat ", " Uno Vivace ", 2013);

        assertEquals("Fiat", alterado.marca());
        assertEquals("Uno Vivace", alterado.modelo());
        assertEquals(2013, alterado.ano());
        assertEquals(maria, alterado.cliente());

        remover.remover("ABC1D23");
        assertTrue(listar.listar().isEmpty());
    }

    @Test
    void veiculoInexistenteNaoEDetalhadoAlteradoNemRemovido() {
        repositorio.remover(Placa.de("ABC1D23"));

        assertThrows(VeiculoNaoEncontradoException.class, () -> detalhar.detalhar("ABC1D23"));
        assertThrows(VeiculoNaoEncontradoException.class, () -> alterar.alterar("ABC1D23", "Fiat", "Uno", 2012));
        assertThrows(VeiculoNaoEncontradoException.class, () -> remover.remover("ABC1D23"));
    }

    @Test
    void placaInvalidaNaoAlteraORepositorio() {
        assertThrows(PlacaInvalidaException.class, () -> detalhar.detalhar("ABC12D3"));
        assertEquals(1, listar.listar().size());
    }

    private static final class VeiculoRepositorioFake implements VeiculoRepositorio {

        private final Map<Placa, Veiculo> veiculos = new HashMap<>();

        @Override
        public Optional<Veiculo> buscarPorPlaca(Placa placa) {
            return Optional.ofNullable(veiculos.get(placa));
        }

        @Override
        public List<Veiculo> listar() {
            return List.copyOf(veiculos.values());
        }

        @Override
        public Veiculo salvar(Veiculo veiculo) {
            veiculos.put(veiculo.placa(), veiculo);
            return veiculo;
        }

        @Override
        public void remover(Placa placa) {
            veiculos.remove(placa);
        }
    }
}
