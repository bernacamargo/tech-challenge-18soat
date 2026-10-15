package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.application.usecase.PecaNaoEncontradaException;
import br.com.grupo117.oficina.catalogoestoque.domain.DinheiroInvalidoException;
import br.com.grupo117.oficina.catalogoestoque.domain.EstoqueInsuficienteException;
import br.com.grupo117.oficina.catalogoestoque.domain.QuantidadeInvalidaException;
import br.com.grupo117.oficina.catalogoestoque.domain.ReservaEncerradaException;
import br.com.grupo117.oficina.infra.api.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {PecaController.class, EstoqueDaOrdemDeServicoController.class})
public class CatalogoEstoqueExceptionHandler {
    @ExceptionHandler(PecaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(PecaNaoEncontradaException erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler({QuantidadeInvalidaException.class, DinheiroInvalidoException.class})
    public ResponseEntity<ErroResponse> requisicaoInvalida(RuntimeException erro) {
        return ResponseEntity.badRequest().body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler({EstoqueInsuficienteException.class, ReservaEncerradaException.class})
    public ResponseEntity<ErroResponse> conflitoDeEstoque(RuntimeException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> gravacaoConcorrente(ObjectOptimisticLockingFailureException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse("Estoque da peca alterado por outra operacao; tente novamente"));
    }
}
