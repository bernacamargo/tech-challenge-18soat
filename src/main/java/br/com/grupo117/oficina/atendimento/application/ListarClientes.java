package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import java.util.List;
import java.util.Objects;

/**
 * Lista os clientes cadastrados.
 */
public final class ListarClientes {

    private final ClienteRepositorio clientes;

    public ListarClientes(ClienteRepositorio clientes) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
    }

    public List<Cliente> listar() {
        return clientes.listar();
    }
}
