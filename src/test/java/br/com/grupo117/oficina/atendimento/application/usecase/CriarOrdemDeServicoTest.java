package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.IdentificarCliente;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpjInvalidoException;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.PlacaInvalidaException;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriarOrdemDeServicoTest {

    private final ClientesFake clientes = new ClientesFake();
    private final VeiculosFake veiculos = new VeiculosFake();
    private final OrdensFake ordens = new OrdensFake();
    private final CriarOrdemDeServico criar = new CriarOrdemDeServico(
            new IdentificarCliente(clientes), new CadastrarVeiculo(clientes, veiculos), veiculos, ordens);

    @Test
    void cadastraClienteEVeiculoEAbreOsRecebida() {
        OrdemDeServico os = abrir("529.982.247-25", "abc-1d23");

        assertThat(os.id()).isNotNull();
        assertThat(os.clienteId()).isEqualTo("52998224725");
        assertThat(os.veiculoId()).isEqualTo("ABC1D23");
        assertThat(os.status()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(ordens.salvas).containsExactly(os);
        assertThat(clientes.listar()).hasSize(1);
        assertThat(veiculos.listar()).hasSize(1);
        assertThat(veiculos.listar().getFirst().cliente().cpfCnpj().digitos()).isEqualTo(os.clienteId());
    }

    @Test
    void reutilizaClienteExistenteAoCadastrarVeiculo() {
        Cliente cliente = Cliente.cadastrar("Nome original", "52998224725");
        clientes.salvar(cliente);

        abrir("52998224725", "ABC1D23");

        assertThat(clientes.listar()).containsExactly(cliente);
        assertThat(clientes.salvamentos).isEqualTo(1);
        assertThat(veiculos.listar().getFirst().cliente()).isSameAs(cliente);
    }

    @Test
    void reutilizaVeiculoEPermiteNovasOsComIdentidadesDistintas() {
        Cliente cliente = Cliente.cadastrar("Maria", "52998224725");
        clientes.salvar(cliente);
        Veiculo veiculo = Veiculo.cadastrar(cliente, "ABC1D23", "Fiat", "Uno", 2012);
        veiculos.salvar(veiculo);

        OrdemDeServico primeira = abrir("52998224725", "ABC1D23");
        OrdemDeServico segunda = abrir("52998224725", "abc-1d23");

        assertThat(primeira.id()).isNotEqualTo(segunda.id());
        assertThat(ordens.salvas).containsExactly(primeira, segunda);
        assertThat(veiculos.listar()).containsExactly(veiculo);
        assertThat(veiculos.salvamentos).isEqualTo(1);
        assertThat(clientes.salvamentos).isEqualTo(1);
    }

    @Test
    void rejeitaVeiculoDeOutroClienteAntesDeCadastrarOuSalvar() {
        Cliente outro = Cliente.cadastrar("João", "12345678909");
        clientes.salvar(outro);
        veiculos.salvar(Veiculo.cadastrar(outro, "ABC1D23", "Fiat", "Uno", 2012));

        assertThatIllegalArgumentException().isThrownBy(() -> abrir("52998224725", "ABC1D23"))
                .withMessage("Veículo não pertence ao cliente informado");

        assertThat(ordens.salvas).isEmpty();
        assertThat(clientes.listar()).containsExactly(outro);
        assertThat(veiculos.salvamentos).isEqualTo(1);
    }

    @Test
    void documentoInvalidoNaoAbreOsNemCadastra() {
        assertThatThrownBy(() -> abrir("11111111111", "ABC1D23"))
                .isInstanceOf(CpfCnpjInvalidoException.class);
        assertSemCadastrosOuOs();
    }

    @Test
    void placaInvalidaNaoAbreOsNemCadastra() {
        assertThatThrownBy(() -> abrir("52998224725", "INVALIDA"))
                .isInstanceOf(PlacaInvalidaException.class);
        assertSemCadastrosOuOs();
    }

    @Test
    void veiculoComDadosInvalidosNaoAbreOs() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> criar.criar("52998224725", "Maria", "ABC1D23", "", "Uno", 2012));
        assertThat(ordens.salvas).isEmpty();
        assertThat(veiculos.listar()).isEmpty();
    }

    @Test
    void falhaAoSalvarVeiculoNaoSalvaOs() {
        VeiculoRepositorio falho = new VeiculosFake() {
            @Override
            public Veiculo salvar(Veiculo veiculo) {
                throw new IllegalStateException("Falha de persistência");
            }
        };
        CriarOrdemDeServico casoDeUso = new CriarOrdemDeServico(new IdentificarCliente(clientes),
                new CadastrarVeiculo(clientes, falho), falho, ordens);

        assertThatThrownBy(() -> casoDeUso.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012))
                .isInstanceOf(IllegalStateException.class).hasMessage("Falha de persistência");
        assertThat(ordens.salvas).isEmpty();
    }

    private OrdemDeServico abrir(String documento, String placa) {
        return criar.criar(documento, "Maria", placa, "Fiat", "Uno", 2012);
    }

    private void assertSemCadastrosOuOs() {
        assertThat(ordens.salvas).isEmpty();
        assertThat(clientes.listar()).isEmpty();
        assertThat(veiculos.listar()).isEmpty();
    }

    private static final class OrdensFake implements OrdemDeServicoRepositorio {
        private final List<OrdemDeServico> salvas = new ArrayList<>();

        @Override
        public Optional<OrdemDeServico> buscarPorId(UUID id) {
            return salvas.stream().filter(os -> os.id().equals(id)).findFirst();
        }

        @Override
        public OrdemDeServico salvar(OrdemDeServico os) {
            salvas.add(os);
            return os;
        }
    }

    private static final class ClientesFake implements ClienteRepositorio {
        private final Map<CpfCnpj, Cliente> dados = new HashMap<>();
        private int salvamentos;

        @Override
        public Optional<Cliente> buscarPorCpfCnpj(CpfCnpj documento) { return Optional.ofNullable(dados.get(documento)); }

        @Override
        public List<Cliente> listar() { return List.copyOf(dados.values()); }

        @Override
        public Cliente salvar(Cliente cliente) {
            salvamentos++;
            dados.put(cliente.cpfCnpj(), cliente);
            return cliente;
        }

        @Override
        public void remover(CpfCnpj documento) { dados.remove(documento); }
    }

    private static class VeiculosFake implements VeiculoRepositorio {
        private final Map<Placa, Veiculo> dados = new HashMap<>();
        private int salvamentos;

        @Override
        public Optional<Veiculo> buscarPorPlaca(Placa placa) { return Optional.ofNullable(dados.get(placa)); }

        @Override
        public List<Veiculo> listar() { return List.copyOf(dados.values()); }

        @Override
        public Veiculo salvar(Veiculo veiculo) {
            salvamentos++;
            dados.put(veiculo.placa(), veiculo);
            return veiculo;
        }

        @Override
        public void remover(Placa placa) { dados.remove(placa); }
    }
}
