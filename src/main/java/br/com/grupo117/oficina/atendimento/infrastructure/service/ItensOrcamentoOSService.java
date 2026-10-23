package br.com.grupo117.oficina.atendimento.infrastructure.service;

import br.com.grupo117.oficina.atendimento.application.usecase.AprovarOrcamento;
import br.com.grupo117.oficina.atendimento.application.usecase.IncluirPecaNaOS;
import br.com.grupo117.oficina.atendimento.application.usecase.IncluirServicoNaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.infrastructure.persistence.OperacaoOSLock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Uma transação PostgreSQL para OS, vínculos, reservas e saldos. */
@Service
@Transactional
public class ItensOrcamentoOSService {
    private final IncluirServicoNaOS servicos;
    private final IncluirPecaNaOS pecas;
    private final AprovarOrcamento aprovacao;
    private final OperacaoOSLock locks;

    public ItensOrcamentoOSService(IncluirServicoNaOS servicos, IncluirPecaNaOS pecas,
                                  AprovarOrcamento aprovacao, OperacaoOSLock locks) {
        this.servicos = servicos;
        this.pecas = pecas;
        this.aprovacao = aprovacao;
        this.locks = locks;
    }

    public OrdemDeServico incluirServico(UUID osId, UUID servicoId, int quantidade) {
        locks.bloquearOrdem(osId);
        return servicos.incluir(osId, servicoId, quantidade);
    }

    public OrdemDeServico incluirPeca(UUID osId, UUID pecaId, int quantidade) {
        locks.bloquearOrdem(osId);
        return pecas.incluir(osId, pecaId, quantidade);
    }

    public OrdemDeServico aprovar(UUID osId) {
        locks.bloquearOrdem(osId);
        return aprovacao.aprovar(osId);
    }
}
