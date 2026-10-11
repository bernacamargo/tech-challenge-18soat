package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Optional;

/**
 * Port de saida do repositorio de clientes. A implementacao (JPA ou outra)
 * fica na infraestrutura; o caso de uso so depende desta interface.
 */
public interface ClienteRepositorio {

    Optional<Cliente> buscarPorCpfCnpj(CpfCnpj cpfCnpj);

    Cliente salvar(Cliente cliente);
}
