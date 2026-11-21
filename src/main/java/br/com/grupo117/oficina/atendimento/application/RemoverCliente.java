package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Objects;

/**
 * Exclui logicamente o cliente identificado pelo CPF/CNPJ.
 */
public final class RemoverCliente {

    private final ClienteRepositorio clientes;
    private final VinculoDeCadastro vinculos;

    public RemoverCliente(ClienteRepositorio clientes) {
        this(clientes, VinculoDeCadastro.nenhum());
    }

    public RemoverCliente(ClienteRepositorio clientes, VinculoDeCadastro vinculos) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
        this.vinculos = Objects.requireNonNull(vinculos, "Consulta de vinculos e obrigatoria");
    }

    public void remover(String documento) {
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        if (clientes.buscarPorCpfCnpj(cpfCnpj).isEmpty()) {
            throw new ClienteNaoEncontradoException(cpfCnpj);
        }
        if (vinculos.clienteEmUso(cpfCnpj)) {
            throw new CadastroComVinculoException();
        }
        clientes.remover(cpfCnpj);
    }
}
