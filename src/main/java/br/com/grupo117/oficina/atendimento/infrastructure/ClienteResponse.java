package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.domain.Cliente;

public record ClienteResponse(String nome, String cpfCnpj) {

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(cliente.nome(), cliente.cpfCnpj().digitos());
    }
}
