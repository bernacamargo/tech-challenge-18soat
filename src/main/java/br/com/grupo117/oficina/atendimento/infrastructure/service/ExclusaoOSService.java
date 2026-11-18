package br.com.grupo117.oficina.atendimento.infrastructure.service;

import br.com.grupo117.oficina.atendimento.application.usecase.ExcluirOrdemDeServico;
import br.com.grupo117.oficina.atendimento.infrastructure.persistence.OperacaoOSLock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExclusaoOSService {
    private final ExcluirOrdemDeServico excluir;
    private final OperacaoOSLock locks;

    public ExclusaoOSService(ExcluirOrdemDeServico excluir, OperacaoOSLock locks) {
        this.excluir = excluir;
        this.locks = locks;
    }

    public void excluir(UUID id) {
        locks.bloquearOrdem(id);
        excluir.excluir(id);
    }
}
