package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import br.com.grupo117.oficina.catalogoestoque.application.port.ServicoRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro;
import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA do port de servicos.
 */
@Component
@Transactional
public class ServicoRepositoryAdapter implements ServicoRepositorio {

    private final ServicoJpaRepository jpa;

    public ServicoRepositoryAdapter(ServicoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Servico> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Servico> listar() {
        return jpa.findAll().stream().map(this::paraDominio).toList();
    }

    @Override
    public Servico salvar(Servico servico) {
        ServicoEntity entity = jpa.findById(servico.id()).orElseGet(ServicoEntity::new);
        entity.setId(servico.id());
        entity.setNome(servico.nome());
        entity.setDescricao(servico.descricao());
        entity.setPreco(servico.preco().valor());
        return paraDominio(jpa.save(entity));
    }

    @Override
    public void remover(UUID id) {
        jpa.deleteById(id);
    }

    private Servico paraDominio(ServicoEntity entity) {
        return new Servico(entity.getId(), entity.getNome(), entity.getDescricao(), Dinheiro.de(entity.getPreco()));
    }
}
