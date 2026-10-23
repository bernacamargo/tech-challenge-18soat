package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.application.usecase.OrdemDeServicoNaoEncontradaException;
import br.com.grupo117.oficina.infra.api.ErroResponse;
import br.com.grupo117.oficina.catalogoestoque.domain.EstoqueInsuficienteException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {OrdemDeServicoController.class, AcompanhamentoOrdemDeServicoController.class, ItensOrcamentoOSController.class})
public class OrdemDeServicoExceptionHandler {
    @ExceptionHandler(OrdemDeServicoNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(OrdemDeServicoNaoEncontradaException erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroResponse> estadoInvalido(IllegalStateException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(erro.getMessage()));
    }
    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<ErroResponse> estoqueInsuficiente(EstoqueInsuficienteException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler({ConcurrencyFailureException.class, OptimisticLockException.class, PessimisticLockException.class})
    public ResponseEntity<ErroResponse> disputaConcorrente(RuntimeException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse("Operação concorrente na OS ou no estoque. Consulte o estado atual e tente novamente."));
    }
}
