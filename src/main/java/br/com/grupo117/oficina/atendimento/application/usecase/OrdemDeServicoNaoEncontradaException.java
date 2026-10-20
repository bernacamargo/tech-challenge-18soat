package br.com.grupo117.oficina.atendimento.application.usecase;

import java.util.UUID;

public final class OrdemDeServicoNaoEncontradaException extends RuntimeException {

    public OrdemDeServicoNaoEncontradaException(UUID id) {
        super("Ordem de serviço não encontrada: " + id);
    }
}
