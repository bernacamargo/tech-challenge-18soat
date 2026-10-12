package br.com.grupo117.oficina.atendimento.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.PlacaInvalidaException;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CadastrarVeiculoTest {

    @Test
    void veiculoCadastradoFicaLigadoAoCliente() {
        ClienteRepositorioFake clientes = new ClienteRepositorioFake();
        Cliente maria = Cliente.cadastrar("Maria Souza", "52998224725");
        clientes.salvar(maria);
        VeiculoRepositorioFake veiculos = new VeiculoRepositorioFake();
        CadastrarVeiculo casoDeUso = new CadastrarVeiculo(clientes, veiculos);

        Veiculo veiculo = casoDeUso.cadastrar("529.982.247-25", "abc-1d23", " Fiat ", " Uno ", 2012);

        assertEquals(maria, veiculo.cliente());
        assertEquals(Placa.de("ABC1D23"), veiculo.placa());
        assertEquals("Fiat", veiculo.marca());
        assertEquals("Uno", veiculo.modelo());
        assertEquals(2012, veiculo.ano());
        assertSame(veiculo, veiculos.buscarPorPlaca(veiculo.placa()).orElseThrow());
    }

    @Test
    void clienteInexistenteNaoCadastraVeiculo() {
        ClienteRepositorioFake clientes = new ClienteRepositorioFake();
        VeiculoRepositorioFake veiculos = new VeiculoRepositorioFake();
        CadastrarVeiculo casoDeUso = new CadastrarVeiculo(clientes, veiculos);

        ClienteNaoEncontradoException erro = assertThrows(
                ClienteNaoEncontradoException.class,
                () -> casoDeUso.cadastrar("52998224725", "ABC1D23", "Fiat", "Uno", 2012)
        );

        assertEquals("Cliente nao encontrado: 52998224725", erro.getMessage());
        assertTrue(veiculos.vazio());
    }

    @Test
    void placaInvalidaNaoCadastraVeiculo() {
        ClienteRepositorioFake clientes = new ClienteRepositorioFake();
        clientes.salvar(Cliente.cadastrar("Maria Souza", "52998224725"));
        VeiculoRepositorioFake veiculos = new VeiculoRepositorioFake();
        CadastrarVeiculo casoDeUso = new CadastrarVeiculo(clientes, veiculos);

        assertThrows(
                PlacaInvalidaException.class,
                () -> casoDeUso.cadastrar("52998224725", "ABC12D3", "Fiat", "Uno", 2012)
        );
        assertTrue(veiculos.vazio());
    }

    private static final class ClienteRepositorioFake implements ClienteRepositorio {

        private final Map<CpfCnpj, Cliente> clientes = new HashMap<>();

        @Override
        public Optional<Cliente> buscarPorCpfCnpj(CpfCnpj cpfCnpj) {
            return Optional.ofNullable(clientes.get(cpfCnpj));
        }

        @Override
        public Cliente salvar(Cliente cliente) {
            clientes.put(cliente.cpfCnpj(), cliente);
            return cliente;
        }
    }

    private static final class VeiculoRepositorioFake implements VeiculoRepositorio {

        private final Map<Placa, Veiculo> veiculos = new HashMap<>();

        @Override
        public Optional<Veiculo> buscarPorPlaca(Placa placa) {
            return Optional.ofNullable(veiculos.get(placa));
        }

        @Override
        public Veiculo salvar(Veiculo veiculo) {
            veiculos.put(veiculo.placa(), veiculo);
            return veiculo;
        }

        boolean vazio() {
            return veiculos.isEmpty();
        }
    }
}
