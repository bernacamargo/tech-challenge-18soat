package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.IdentificarCliente;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.Objects;
import java.util.UUID;

/** Abre uma OS após identificar o cliente e localizar ou cadastrar seu veículo. */
public final class CriarOrdemDeServico {

    private final IdentificarCliente identificarCliente;
    private final CadastrarVeiculo cadastrarVeiculo;
    private final VeiculoRepositorio veiculos;
    private final OrdemDeServicoRepositorio ordens;

    public CriarOrdemDeServico(IdentificarCliente identificarCliente, CadastrarVeiculo cadastrarVeiculo,
                              VeiculoRepositorio veiculos, OrdemDeServicoRepositorio ordens) {
        this.identificarCliente = Objects.requireNonNull(identificarCliente);
        this.cadastrarVeiculo = Objects.requireNonNull(cadastrarVeiculo);
        this.veiculos = Objects.requireNonNull(veiculos);
        this.ordens = Objects.requireNonNull(ordens);
    }

    public OrdemDeServico criar(String documento, String nome, String placa, String marca, String modelo, int ano) {
        CpfCnpj clienteId = CpfCnpj.de(documento);
        Placa veiculoId = Placa.de(placa);
        Veiculo existente = veiculos.buscarPorPlaca(veiculoId).orElse(null);
        if (existente != null && !existente.cliente().cpfCnpj().equals(clienteId)) {
            throw new IllegalArgumentException("Veículo não pertence ao cliente informado");
        }

        identificarCliente.identificar(documento, nome);
        if (existente == null) {
            cadastrarVeiculo.cadastrar(documento, placa, marca, modelo, ano);
        }
        return ordens.salvar(new OrdemDeServico(UUID.randomUUID(), clienteId.digitos(), veiculoId.valor()));
    }
}
