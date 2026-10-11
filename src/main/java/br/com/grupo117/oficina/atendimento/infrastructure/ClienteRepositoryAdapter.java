package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA do port de clientes.
 */
@Component
@Transactional
public class ClienteRepositoryAdapter implements ClienteRepositorio {

    private final ClienteJpaRepository jpa;

    public ClienteRepositoryAdapter(ClienteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorCpfCnpj(CpfCnpj cpfCnpj) {
        return jpa.findById(cpfCnpj.digitos()).map(this::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return jpa.findAll().stream().map(this::paraDominio).toList();
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        String digitos = cliente.cpfCnpj().digitos();
        ClienteEntity entity = jpa.findById(digitos).orElseGet(ClienteEntity::new);
        entity.setCpfCnpj(digitos);
        entity.setNome(cliente.nome());
        return paraDominio(jpa.save(entity));
    }

    @Override
    public void remover(CpfCnpj cpfCnpj) {
        jpa.deleteById(cpfCnpj.digitos());
    }

    private Cliente paraDominio(ClienteEntity entity) {
        return new Cliente(entity.getNome(), CpfCnpj.de(entity.getCpfCnpj()));
    }
}
