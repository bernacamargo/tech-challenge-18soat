package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.application.usecase.OrdemDeServicoNaoEncontradaException;
import br.com.grupo117.oficina.infra.api.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {OrdemDeServicoController.class, AcompanhamentoOrdemDeServicoController.class})
public class OrdemDeServicoExceptionHandler {
    @ExceptionHandler(OrdemDeServicoNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(OrdemDeServicoNaoEncontradaException erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(erro.getMessage()));
    }
}
