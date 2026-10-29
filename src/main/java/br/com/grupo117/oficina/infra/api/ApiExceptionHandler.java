package br.com.grupo117.oficina.infra.api;

import br.com.grupo117.oficina.atendimento.application.ClienteJaCadastradoException;
import br.com.grupo117.oficina.atendimento.application.ClienteNaoEncontradoException;
import br.com.grupo117.oficina.atendimento.application.VeiculoJaCadastradoException;
import br.com.grupo117.oficina.atendimento.application.VeiculoNaoEncontradoException;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpjInvalidoException;
import br.com.grupo117.oficina.atendimento.domain.PlacaInvalidaException;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({CpfCnpjInvalidoException.class, PlacaInvalidaException.class, IllegalArgumentException.class})
    ResponseEntity<ErroResponse> requisicaoInvalida(RuntimeException erro) {
        return ResponseEntity.badRequest().body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> camposInvalidos(MethodArgumentNotValidException erro) {
        String mensagem = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .reduce((primeira, seguinte) -> primeira + "; " + seguinte)
                .orElse("Requisicao invalida");
        return ResponseEntity.badRequest().body(new ErroResponse(mensagem));
    }

    @ExceptionHandler({ClienteNaoEncontradoException.class, VeiculoNaoEncontradoException.class})
    ResponseEntity<ErroResponse> naoEncontrado(RuntimeException erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler({ClienteJaCadastradoException.class, VeiculoJaCadastradoException.class})
    ResponseEntity<ErroResponse> jaCadastrado(RuntimeException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(erro.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> integridadeReferencial(DataIntegrityViolationException erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(
                        "Cadastro possui vinculos com outros registros e nao pode ser removido ou alterado"));
            }
        }
        // Outras falhas de persistencia nao devem ser mascaradas como conflito de negocio.
        throw erro;
    }

}
