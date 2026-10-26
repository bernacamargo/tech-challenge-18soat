package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.domain.Cliente;

public record ClienteResponse(String nome, String cpfCnpj, String email) {

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(cliente.nome(), cliente.cpfCnpj().digitos(), cliente.email().map(e -> e.valor()).orElse(null));
    }
}
