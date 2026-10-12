package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.domain.Veiculo;

public record VeiculoResponse(String placa, String marca, String modelo, int ano, String cpfCnpjCliente) {

    public static VeiculoResponse de(Veiculo veiculo) {
        return new VeiculoResponse(
                veiculo.placa().valor(),
                veiculo.marca(),
                veiculo.modelo(),
                veiculo.ano(),
                veiculo.cliente().cpfCnpj().digitos()
        );
    }
}
