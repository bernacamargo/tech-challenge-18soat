package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class OrdemDeServicoRepositoryAdapter implements OrdemDeServicoRepositorio {
    private final OrdemDeServicoJpaRepository jpa;

    public OrdemDeServicoRepositoryAdapter(OrdemDeServicoJpaRepository jpa) { this.jpa = jpa; }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrdemDeServico> buscarPorId(UUID id) {
        return jpa.findById(id).map(OrdemDeServicoEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServico> listar() {
        return jpa.findAll(Sort.by("id")).stream().map(OrdemDeServicoEntity::paraDominio).toList();
    }

    @Override
    public OrdemDeServico salvar(OrdemDeServico os) {
        return jpa.saveAndFlush(OrdemDeServicoEntity.de(os)).paraDominio();
    }
}
