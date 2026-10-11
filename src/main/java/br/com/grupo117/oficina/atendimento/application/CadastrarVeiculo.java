package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.Objects;

/**
 * Cadastra um veiculo para um cliente que ja existe. Cliente ausente impede o cadastro.
 */
public final class CadastrarVeiculo {

    private final ClienteRepositorio clientes;
    private final VeiculoRepositorio veiculos;

    public CadastrarVeiculo(ClienteRepositorio clientes, VeiculoRepositorio veiculos) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
        this.veiculos = Objects.requireNonNull(veiculos, "Repositorio de veiculos e obrigatorio");
    }

    public Veiculo cadastrar(String documentoCliente, String placa, String marca, String modelo, int ano) {
        CpfCnpj cpfCnpj = CpfCnpj.de(documentoCliente);
        Cliente cliente = clientes.buscarPorCpfCnpj(cpfCnpj)
                .orElseThrow(() -> new ClienteNaoEncontradoException(cpfCnpj));
        Veiculo veiculo = Veiculo.cadastrar(cliente, placa, marca, modelo, ano);
        if (veiculos.buscarPorPlaca(veiculo.placa()).isPresent()) {
            throw new VeiculoJaCadastradoException(veiculo.placa());
        }
        return veiculos.salvar(veiculo);
    }
}
