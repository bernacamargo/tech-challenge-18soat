package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import br.com.grupo117.oficina.atendimento.application.usecase.OrdemDeServicoNaoEncontradaException;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence.PecaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Serializa comandos de Atendimento; os locks permanecem até o commit da transação. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class OperacaoOSLock {
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    public OperacaoOSLock(EntityManager em, JdbcTemplate jdbc) { this.em = em; this.jdbc = jdbc; }

    public void bloquearOrdem(UUID id) {
        OrdemDeServicoEntity entity = em.find(OrdemDeServicoEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) throw new OrdemDeServicoNaoEncontradaException(id);
        em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
    }

    public void bloquearPeca(UUID id) {
        var encontradas = jdbc.query("select id from peca where id = ? for update",
                (rs, n) -> rs.getObject("id", UUID.class), id);
        if (encontradas.isEmpty()) throw new IllegalStateException("Peça da reserva não encontrada: " + id);
        // Bloquear antes de atualizar o cache JPA evita promover lock sobre versão antiga.
        PecaEntity entity = em.find(PecaEntity.class, id);
        em.refresh(entity);
    }
}
