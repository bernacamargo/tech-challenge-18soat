package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.IdentificarCliente;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.ItemCatalogo;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.util.Objects;
import java.util.UUID;

/** Abre uma OS após identificar o cliente e localizar ou cadastrar seu veículo. */
public final class CriarOrdemDeServico {

    private final IdentificarCliente identificarCliente;
    private final CadastrarVeiculo cadastrarVeiculo;
    private final VeiculoRepositorio veiculos;
    private final OrdemDeServicoRepositorio ordens;
    private final CatalogoOSPort catalogo;
    private final ReservaEstoquePort estoque;

    public CriarOrdemDeServico(IdentificarCliente identificarCliente, CadastrarVeiculo cadastrarVeiculo,
                              VeiculoRepositorio veiculos, OrdemDeServicoRepositorio ordens,
                              CatalogoOSPort catalogo, ReservaEstoquePort estoque) {
        this.identificarCliente = Objects.requireNonNull(identificarCliente);
        this.cadastrarVeiculo = Objects.requireNonNull(cadastrarVeiculo);
        this.veiculos = Objects.requireNonNull(veiculos);
        this.ordens = Objects.requireNonNull(ordens);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.estoque = Objects.requireNonNull(estoque);
    }

    public OrdemDeServico criar(String documento, String nome, String placa, String marca, String modelo, int ano) {
        return criar(documento, nome, placa, marca, modelo, ano, null, null, null);
    }

    public OrdemDeServico criar(String documento, String nome, String placa, String marca, String modelo, int ano,
                               UUID servicoId, UUID pecaId, Integer quantidade) {
        return criar(documento, nome, placa, marca, modelo, ano, servicoId, pecaId, quantidade, null);
    }

    public OrdemDeServico criar(String documento, String nome, String placa, String marca, String modelo, int ano,
                               UUID servicoId, UUID pecaId, Integer quantidade, String email) {
        CpfCnpj clienteId = CpfCnpj.de(documento);
        Placa veiculoId = Placa.de(placa);
        Veiculo existente = veiculos.buscarPorPlaca(veiculoId).orElse(null);
        if (existente != null && !existente.cliente().cpfCnpj().equals(clienteId)) {
            throw new IllegalArgumentException("Veículo não pertence ao cliente informado");
        }

        identificarCliente.identificar(documento, nome, email);
        if (existente == null) {
            cadastrarVeiculo.cadastrar(documento, placa, marca, modelo, ano);
        }
        OrdemDeServico os = ordens.salvar(new OrdemDeServico(UUID.randomUUID(), clienteId.digitos(), veiculoId.valor()));
        if (servicoId == null && pecaId == null) {
            return os;
        }
        apontarItem(os, servicoId, pecaId, quantidade);
        return ordens.salvar(os);
    }

    private void apontarItem(OrdemDeServico os, UUID servicoId, UUID pecaId, Integer quantidade) {
        if (servicoId != null && pecaId != null) {
            throw new IllegalArgumentException("Aponte um serviço ou uma peça");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        if (servicoId != null) {
            ItemCatalogo dados = catalogo.buscarServico(servicoId)
                    .orElseThrow(() -> new IllegalArgumentException("Item não encontrado no catálogo: " + servicoId));
            os.incluirItem(new ItemDaOS(UUID.randomUUID(), servicoId, TipoItemOS.SERVICO,
                    dados.descricao(), quantidade, new Dinheiro(dados.preco())));
            return;
        }
        ItemCatalogo dados = catalogo.buscarPeca(pecaId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado no catálogo: " + pecaId));
        ItemDaOS item = new ItemDaOS(UUID.randomUUID(), pecaId, TipoItemOS.PECA,
                dados.descricao(), quantidade, new Dinheiro(dados.preco()));
        if (!estoque.reservar(os.id(), item.id(), pecaId, quantidade)) {
            throw new IllegalStateException("Estoque insuficiente para a peça: " + pecaId);
        }
        os.incluirItem(item);
    }
}
