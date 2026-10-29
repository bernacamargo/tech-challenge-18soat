package br.com.grupo117.oficina;

import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limparEstoque() { jdbc.execute("TRUNCATE TABLE peca CASCADE"); }

    @Test
    void exclusaoComVinculosRetornaConflitoEPreservaHistorico() {
        var os = criarOs(dadosOs());
        var peca = criarPeca();
        assertThat(chamar(HttpMethod.POST, "/api/ordens-de-servico/" + os + "/pecas",
                Map.of("pecaId", peca, "quantidade", 2)).getStatusCode()).isEqualTo(HttpStatus.OK);
        for (String rota : new String[]{"/api/clientes/52998224725", "/api/veiculos/ABC1D23", "/api/pecas/" + peca}) {
            var resposta = chamar(HttpMethod.DELETE, rota, null);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resposta.getBody()).containsEntry("mensagem",
                    "Cadastro possui vinculos com outros registros e nao pode ser removido ou alterado");
            assertThat(chamar(HttpMethod.GET, rota, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(chamar(HttpMethod.GET, "/api/ordens-de-servico/" + os, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("SELECT quantidade_reservada FROM peca WHERE id = ?", Integer.class, peca))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_de_estoque WHERE peca_id = ?", Integer.class, peca))
                .isEqualTo(1);
    }

    @Test
    void permiteExcluirCadastrosSemHistoricoNaOrdemDosVinculos() {
        chamar(HttpMethod.POST, "/api/clientes", Map.of("nome", "Maria", "cpfCnpj", "52998224725"));
        assertThat(chamar(HttpMethod.POST, "/api/veiculos", Map.of("cpfCnpjCliente", "52998224725", "placa", "ABC1D23",
                "marca", "Fiat", "modelo", "Uno", "ano", 2012)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(chamar(HttpMethod.DELETE, "/api/clientes/52998224725", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(chamar(HttpMethod.DELETE, "/api/veiculos/ABC1D23", null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(HttpMethod.DELETE, "/api/clientes/52998224725", null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(HttpMethod.DELETE, "/api/pecas/" + criarPeca(), null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @ParameterizedTest
    @MethodSource("dadosPecaInvalidos")
    void validaLimitesDaPecaNoCadastroENaAlteracao(String campo, Object valor) {
        var id = criarPeca();
        var dados = dadosPeca();
        dados.put(campo, valor);
        assertThat(chamar(HttpMethod.POST, "/api/pecas", dados).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chamar(HttpMethod.PUT, "/api/pecas/" + id, dados).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM peca", Integer.class)).isEqualTo(1);
        var preservada = chamar(HttpMethod.GET, "/api/pecas/" + id, null).getBody();
        assertThat(preservada).containsEntry("nome", "Oleo").containsEntry("quantidadeDisponivel", 7);
        assertThat(preservada.get("precoUnitario").toString()).isEqualTo("25.25");
    }

    static Stream<Arguments> dadosPecaInvalidos() {
        return Stream.of(Arguments.of("nome", "a".repeat(151)), Arguments.of("descricao", "a".repeat(501)),
                Arguments.of("precoUnitario", "100000000"), Arguments.of("precoUnitario", "0.001"),
                Arguments.of("precoUnitario", "25.251"));
    }

    @Test
    void aceitaLimitesMaximosDeTextoEPrecoNaPeca() {
        var dados = dadosPeca();
        dados.put("nome", "a".repeat(150));
        dados.put("descricao", "a".repeat(500));
        dados.put("precoUnitario", "99999999.99");
        var resposta = chamar(HttpMethod.POST, "/api/pecas", dados);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(new java.math.BigDecimal(resposta.getBody().get("precoUnitario").toString()))
                .isEqualByComparingTo("99999999.99");
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "marca", "modelo"})
    void aberturaDaOsValidaTamanhosAntesDeCadastrarClienteEVeiculo(String campo) {
        var dados = dadosOs();
        dados.put(campo, "a".repeat(campo.equals("nome") ? 151 : 81));
        assertThat(chamar(HttpMethod.POST, "/api/ordens-de-servico", dados).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cliente", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM veiculo", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ordem_de_servico", Integer.class)).isZero();
    }

    @Test
    void aberturaDaOsAceitaTamanhosMaximos() {
        var dados = dadosOs();
        dados.put("nome", "a".repeat(150));
        dados.put("marca", "a".repeat(80));
        dados.put("modelo", "a".repeat(80));
        criarOs(dados);
    }

    @Test
    void overflowDeEntradaRetorna400EPreservaEstoque() {
        UUID id = criarPeca();
        var resposta = chamar(HttpMethod.POST, "/api/pecas/" + id + "/entradas", Map.of("quantidade", Integer.MAX_VALUE));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("mensagem", "Entrada excede o limite de quantidade em estoque");
        assertThat(chamar(HttpMethod.GET, "/api/pecas/" + id, null).getBody())
                .containsEntry("quantidadeDisponivel", 7).containsEntry("quantidadeReservada", 0);
    }

    private UUID criarOs(Map<String, Object> dados) {
        var resposta = chamar(HttpMethod.POST, "/api/ordens-de-servico", dados);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(resposta.getBody().get("id").toString());
    }

    private UUID criarPeca() {
        var resposta = chamar(HttpMethod.POST, "/api/pecas", dadosPeca());
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(resposta.getBody().get("id").toString());
    }

    private Map<String, Object> dadosPeca() {
        return new HashMap<>(Map.of("nome", "Oleo", "tipo", "INSUMO", "precoUnitario", "25.25", "quantidadeInicial", 7));
    }

    private Map<String, Object> dadosOs() {
        return new HashMap<>(Map.of("documento", "52998224725", "nome", "Maria", "placa", "ABC1D23",
                "marca", "Fiat", "modelo", "Uno", "ano", 2012));
    }

    private ResponseEntity<Map> chamar(HttpMethod metodo, String rota, Object corpo) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(JwtTestSupport.token("ADMINISTRADOR"));
        return restTemplate.exchange(rota, metodo, new HttpEntity<>(corpo, headers), Map.class);
    }
}
