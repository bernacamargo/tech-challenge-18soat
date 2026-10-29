package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Matriz de autorizacao por Perfil (D7, D8): sentinelas dos @PreAuthorize nos dois contextos,
 * regra de URL de clientes e veiculos (nega antes da validacao do corpo), piso da cadeia
 * (token sem Perfil da oficina recebe 403 ate em rota sem handler) e rotas publicas preservadas.
 */
class AutorizacaoPorPerfilIntegrationTest extends AbstractIntegrationTest {

    private static final String ANONIMO = null;

    @ParameterizedTest(name = "{0}")
    @MethodSource("matriz")
    void respeitaAMatrizDeAutorizacao(String caso, String token, HttpMethod metodo, String rota,
                                      Object corpo, HttpStatus esperado) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (corpo != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        ResponseEntity<String> resposta = restTemplate.exchange(
                rota, metodo, new HttpEntity<>(corpo, headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(esperado);
    }

    static Stream<Arguments> matriz() {
        // tokens emitidos na hora da execucao (exp de 300 s), nao na carga da classe
        String atendente = JwtTestSupport.token("ATENDENTE");
        String mecanico = JwtTestSupport.token("MECANICO");
        String administrador = JwtTestSupport.token("ADMINISTRADOR");
        String semPerfil = JwtTestSupport.tokenSemPerfilDaOficina();
        String peca = "/api/pecas/" + UUID.randomUUID();
        String entrega = "/api/ordens-de-servico/" + UUID.randomUUID() + "/entrega";
        Map<String, Object> clienteValido = Map.of("nome", "Maria Souza", "cpfCnpj", "529.982.247-25");
        Map<String, Object> alterarCliente = Map.of("nome", "Maria Souza");
        Map<String, Object> veiculoValido = Map.of("cpfCnpjCliente", "529.982.247-25", "placa", "ABC1D23",
                "marca", "Fiat", "modelo", "Uno", "ano", 2012);
        Map<String, Object> alterarVeiculo = Map.of("marca", "Fiat", "modelo", "Uno", "ano", 2012);

        return Stream.of(
                // C1-C4: sentinelas dos @PreAuthorize (catalogoestoque e atendimento) e controles positivos
                Arguments.of("sentinela-catalogo-atendente-remove-peca",
                        atendente, HttpMethod.DELETE, peca, null, HttpStatus.FORBIDDEN),
                Arguments.of("sentinela-catalogo-administrador-chega-ao-caso-de-uso",
                        administrador, HttpMethod.DELETE, peca, null, HttpStatus.NOT_FOUND),
                Arguments.of("sentinela-atendimento-mecanico-registra-entrega",
                        mecanico, HttpMethod.POST, entrega, null, HttpStatus.FORBIDDEN),
                Arguments.of("sentinela-atendimento-administrador-chega-ao-caso-de-uso",
                        administrador, HttpMethod.POST, entrega, null, HttpStatus.NOT_FOUND),
                // C5-C12: regra de URL de clientes e veiculos nega antes do @Valid
                Arguments.of("mecanico-cria-cliente-corpo-valido",
                        mecanico, HttpMethod.POST, "/api/clientes", clienteValido, HttpStatus.FORBIDDEN),
                Arguments.of("mecanico-cria-cliente-corpo-vazio",
                        mecanico, HttpMethod.POST, "/api/clientes", Map.of(), HttpStatus.FORBIDDEN),
                Arguments.of("mecanico-altera-cliente",
                        mecanico, HttpMethod.PUT, "/api/clientes/52998224725", alterarCliente, HttpStatus.FORBIDDEN),
                Arguments.of("mecanico-remove-cliente",
                        mecanico, HttpMethod.DELETE, "/api/clientes/52998224725", null, HttpStatus.FORBIDDEN),
                Arguments.of("mecanico-lista-veiculos",
                        mecanico, HttpMethod.GET, "/api/veiculos", null, HttpStatus.FORBIDDEN),
                Arguments.of("mecanico-cria-veiculo-corpo-valido",
                        mecanico, HttpMethod.POST, "/api/veiculos", veiculoValido, HttpStatus.FORBIDDEN),
                Arguments.of("mecanico-altera-veiculo",
                        mecanico, HttpMethod.PUT, "/api/veiculos/ABC1D23", alterarVeiculo, HttpStatus.FORBIDDEN),
                Arguments.of("mecanico-remove-veiculo",
                        mecanico, HttpMethod.DELETE, "/api/veiculos/ABC1D23", null, HttpStatus.FORBIDDEN),
                // C13-C14: controles positivos da regra de URL
                Arguments.of("atendente-lista-clientes",
                        atendente, HttpMethod.GET, "/api/clientes", null, HttpStatus.OK),
                Arguments.of("administrador-lista-veiculos",
                        administrador, HttpMethod.GET, "/api/veiculos", null, HttpStatus.OK),
                // C15-C17: piso (o trio prova que o 403 vem da cadeia, nao de um handler)
                Arguments.of("sem-perfil-rota-inexistente-piso",
                        semPerfil, HttpMethod.GET, "/api/rota-inexistente", null, HttpStatus.FORBIDDEN),
                Arguments.of("atendente-rota-inexistente-oraculo",
                        atendente, HttpMethod.GET, "/api/rota-inexistente", null, HttpStatus.NOT_FOUND),
                Arguments.of("anonimo-rota-inexistente",
                        ANONIMO, HttpMethod.GET, "/api/rota-inexistente", null, HttpStatus.UNAUTHORIZED),
                // C18-C19: sem Perfil da oficina nas rotas administrativas
                Arguments.of("sem-perfil-clientes",
                        semPerfil, HttpMethod.GET, "/api/clientes", null, HttpStatus.FORBIDDEN),
                Arguments.of("sem-perfil-ordens-de-servico",
                        semPerfil, HttpMethod.GET, "/api/ordens-de-servico", null, HttpStatus.FORBIDDEN),
                // C20, C21, C24: rotas publicas preservadas
                Arguments.of("anonimo-health",
                        ANONIMO, HttpMethod.GET, "/api/health", null, HttpStatus.OK),
                Arguments.of("anonimo-api-docs",
                        ANONIMO, HttpMethod.GET, "/v3/api-docs", null, HttpStatus.OK),
                Arguments.of("anonimo-acompanhamento-publico",
                        ANONIMO, HttpMethod.GET, "/api/acompanhamento/ordens-de-servico/" + UUID.randomUUID(),
                        null, HttpStatus.NOT_FOUND));
    }
}
