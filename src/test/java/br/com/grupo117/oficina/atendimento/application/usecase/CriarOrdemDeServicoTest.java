package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.IdentificarCliente;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.ItemCatalogo;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpjInvalidoException;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.PlacaInvalidaException;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.math.BigDecimal;
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
    private final CriarOrdemDeServico criar = novoCaso(veiculos, ordens, catalogoVazio(), (a, b, c, d) -> false);

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
    void apontaServicoDaListaDeDisponibilidadeAoAbrir() {
        UUID servicoId = UUID.randomUUID();
        CriarOrdemDeServico caso = novoCaso(veiculos, ordens, catalogo(servicoId, null), (a, b, c, d) -> false);

        OrdemDeServico os = caso.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012, servicoId, null, 2);

        assertThat(os.status()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(os.itens()).singleElement().satisfies(item -> {
            assertThat(item.tipo()).isEqualTo(TipoItemOS.SERVICO);
            assertThat(item.catalogoId()).isEqualTo(servicoId);
            assertThat(item.quantidade()).isEqualTo(2);
            assertThat(item.descricao()).isEqualTo("Troca de óleo");
        });
    }

    @Test
    void apontaPecaEReservaEstoqueAoAbrir() {
        UUID pecaId = UUID.randomUUID();
        List<UUID> reservas = new ArrayList<>();
        CriarOrdemDeServico caso = novoCaso(veiculos, ordens, catalogo(null, pecaId),
                (osId, itemId, peca, quantidade) -> { reservas.add(peca); return true; });

        OrdemDeServico os = caso.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012, null, pecaId, 3);

        assertThat(reservas).containsExactly(pecaId);
        assertThat(os.itens()).singleElement().extracting(ItemDaOS::tipo).isEqualTo(TipoItemOS.PECA);
        assertThat(os.status()).isEqualTo(StatusOS.RECEBIDA);
    }

    @Test
    void estoqueInsuficienteNaoIncluiAPecaApontada() {
        UUID pecaId = UUID.randomUUID();
        CriarOrdemDeServico caso = novoCaso(veiculos, ordens, catalogo(null, pecaId), (a, b, c, d) -> false);

        assertThatThrownBy(() -> caso.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012, null, pecaId, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(ordens.salvas.getFirst().itens()).isEmpty();
    }

    @Test
    void recusaApontarServicoEPecaJuntosOuSemQuantidade() {
        UUID id = UUID.randomUUID();
        CriarOrdemDeServico caso = novoCaso(veiculos, ordens, catalogo(id, id), (a, b, c, d) -> true);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> caso.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012, id, id, 1))
                .withMessage("Aponte um serviço ou uma peça");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> caso.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012, id, null, null))
                .withMessage("Quantidade deve ser positiva");
    }

    private CriarOrdemDeServico novoCaso(VeiculoRepositorio veiculosDoCaso, OrdemDeServicoRepositorio ordensDoCaso,
                                        CatalogoOSPort catalogo, br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort estoque) {
        return new CriarOrdemDeServico(new IdentificarCliente(clientes), new CadastrarVeiculo(clientes, veiculosDoCaso),
                veiculosDoCaso, ordensDoCaso, catalogo, estoque);
    }

    private static CatalogoOSPort catalogoVazio() {
        return catalogo(null, null);
    }

    private static CatalogoOSPort catalogo(UUID servicoId, UUID pecaId) {
        return new CatalogoOSPort() {
            @Override
            public Optional<ItemCatalogo> buscarServico(UUID id) {
                return servicoId != null && servicoId.equals(id)
                        ? Optional.of(new ItemCatalogo(id, "Troca de óleo", new BigDecimal("80.50")))
                        : Optional.empty();
            }

            @Override
            public Optional<ItemCatalogo> buscarPeca(UUID id) {
                return pecaId != null && pecaId.equals(id)
                        ? Optional.of(new ItemCatalogo(id, "Óleo", new BigDecimal("25.25")))
                        : Optional.empty();
            }
        };
    }

    @Test
    void falhaAoSalvarVeiculoNaoSalvaOs() {
        VeiculoRepositorio falho = new VeiculosFake() {
            @Override
            public Veiculo salvar(Veiculo veiculo) {
                throw new IllegalStateException("Falha de persistência");
            }
        };
        CriarOrdemDeServico casoDeUso = novoCaso(falho, ordens, catalogoVazio(), (a, b, c, d) -> false);

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
        public List<OrdemDeServico> listar() {
            return List.copyOf(salvas);
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
