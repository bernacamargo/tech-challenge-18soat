package br.com.grupo117.oficina.infra.api;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void traduzChaveEstrangeiraSemExporDadosDaExcecao() {
        var erro = new DataIntegrityViolationException("detalhes internos",
                new RuntimeException(new SQLException("identificadores internos", "23503")));
        var resposta = handler.integridadeReferencial(erro);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().mensagem()).contains("vinculos").doesNotContain("internos");
    }

    @Test
    void naoMascaraOutrasFalhasDePersistencia() {
        var erro = new DataIntegrityViolationException("restricao de tamanho",
                new SQLException("valor muito longo", "22001"));
        assertThatThrownBy(() -> handler.integridadeReferencial(erro)).isSameAs(erro);
    }

    @Test
    void naoTraduzExcecaoSemCausaSqlComoConflito() {
        var erro = new DataIntegrityViolationException("erro desconhecido");
        assertThatThrownBy(() -> handler.integridadeReferencial(erro)).isSameAs(erro);
    }
}
