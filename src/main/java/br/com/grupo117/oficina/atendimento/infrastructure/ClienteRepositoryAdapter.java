package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.ClienteJaCadastradoException;
import br.com.grupo117.oficina.atendimento.application.ClienteNaoEncontradoException;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.Email;
import java.time.Instant;
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
        return jpa.findByCpfCnpjAndExcluidaEmIsNull(cpfCnpj.digitos()).map(this::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return jpa.findAllByExcluidaEmIsNull().stream().map(this::paraDominio).toList();
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        String digitos = cliente.cpfCnpj().digitos();
        ClienteEntity entity = jpa.findById(digitos).orElseGet(ClienteEntity::new);
        if (entity.getExcluidaEm() != null) {
            throw new ClienteJaCadastradoException(cliente.cpfCnpj());
        }
        entity.setCpfCnpj(digitos);
        entity.setNome(cliente.nome());
        entity.setEmail(cliente.email().map(e -> e.valor()).orElse(null));
        return paraDominio(jpa.save(entity));
    }

    @Override
    public void remover(CpfCnpj cpfCnpj) {
        ClienteEntity entity = jpa.findByCpfCnpjAndExcluidaEmIsNull(cpfCnpj.digitos())
                .orElseThrow(() -> new ClienteNaoEncontradoException(cpfCnpj));
        entity.setExcluidaEm(Instant.now());
        jpa.save(entity);
    }

    private Cliente paraDominio(ClienteEntity entity) {
        return new Cliente(entity.getNome(), CpfCnpj.de(entity.getCpfCnpj()),
                entity.getEmail() == null ? null : new Email(entity.getEmail()));
    }
}
