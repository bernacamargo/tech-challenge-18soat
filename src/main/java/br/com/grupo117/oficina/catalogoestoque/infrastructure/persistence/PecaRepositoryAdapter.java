package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste a peca reaproveitando a entidade gerenciada da transacao: assim a versao lida
 * na consulta e a usada no UPDATE, e uma gravacao concorrente falha com lock otimista.
 */
@Repository
@Transactional
public class PecaRepositoryAdapter implements PecaRepositorio {
    private final PecaJpaRepository jpa;

    public PecaRepositoryAdapter(PecaJpaRepository jpa) { this.jpa = jpa; }

    @Override
    @Transactional(readOnly = true)
    public Optional<Peca> buscarPorId(UUID id) {
        return jpa.findById(id).map(PecaEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Peca> listar() {
        return jpa.findAll(Sort.by("nome", "id")).stream().map(PecaEntity::paraDominio).toList();
    }

    @Override
    public Peca salvar(Peca peca) {
        PecaEntity entity = jpa.findById(peca.id()).orElseGet(() -> PecaEntity.nova(peca.id()));
        entity.atualizar(peca);
        return jpa.saveAndFlush(entity).paraDominio();
    }

    @Override
    public void remover(UUID id) {
        jpa.deleteById(id);
        jpa.flush();
    }
}
