package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.atendimento.infrastructure.ClienteResponse;
import br.com.grupo117.oficina.atendimento.infrastructure.VeiculoResponse;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class ExclusaoClienteVeiculoIntegrationTest extends AbstractIntegrationTest {

    private static final String CPF = "52998224725";
    private static final String PLACA = "ABC1D23";
    private static final String VINCULO =
            "Cadastro possui vinculos com outros registros e nao pode ser removido ou alterado";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrdemDeServicoService ordens;

    @Test
    void deleteMarcaExclusaoESomeDasListagens() {
        HttpHeaders atendente = headers("ATENDENTE");
        criarCliente(atendente);
        criarVeiculo(atendente);

        assertThat(excluir(atendente, "/api/veiculos/" + PLACA).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(buscar(atendente, "/api/veiculos/" + PLACA).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listarVeiculos(atendente)).isEmpty();
        assertThat(marcado("veiculo", "placa", PLACA)).isEqualTo(1);
        assertThat(excluir(atendente, "/api/veiculos/" + PLACA).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(excluir(atendente, "/api/clientes/" + CPF).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(buscar(atendente, "/api/clientes/" + CPF).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listarClientes(atendente)).isEmpty();
        assertThat(marcado("cliente", "cpf_cnpj", CPF)).isEqualTo(1);
        assertThat(excluir(atendente, "/api/clientes/" + CPF).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> recadastro = restTemplate.exchange(
                "/api/clientes",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Outra", "cpfCnpj", CPF), atendente),
                String.class);
        assertThat(recadastro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(listarClientes(atendente)).isEmpty();
        assertThat(linhas("cliente", "cpf_cnpj", CPF)).isEqualTo(1);
    }

    @Test
    void veiculoAtivoBloqueiaClienteEOsBloqueiaAmbos() {
        HttpHeaders atendente = headers("ATENDENTE");
        criarCliente(atendente);
        criarVeiculo(atendente);

        ResponseEntity<String> clienteComVeiculo = excluir(atendente, "/api/clientes/" + CPF);
        assertThat(clienteComVeiculo.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(clienteComVeiculo.getBody()).contains(VINCULO);
        assertThat(marcado("cliente", "cpf_cnpj", CPF)).isZero();
        assertThat(listarClientes(atendente)).hasSize(1);

        ordens.criar(CPF, "Maria Souza", PLACA, "Fiat", "Uno", 2012);

        ResponseEntity<String> veiculoComOs = excluir(atendente, "/api/veiculos/" + PLACA);
        assertThat(veiculoComOs.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(veiculoComOs.getBody()).contains(VINCULO);
        assertThat(buscar(atendente, "/api/veiculos/" + PLACA).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(buscar(atendente, "/api/clientes/" + CPF).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(marcado("veiculo", "placa", PLACA)).isZero();
        assertThat(marcado("cliente", "cpf_cnpj", CPF)).isZero();
    }

    @Test
    void mecanicoRecebe403ENaoMarcaExclusao() {
        HttpHeaders atendente = headers("ATENDENTE");
        HttpHeaders mecanico = headers("MECANICO");
        criarCliente(atendente);
        criarVeiculo(atendente);

        assertThat(excluir(mecanico, "/api/clientes/" + CPF).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(excluir(mecanico, "/api/veiculos/" + PLACA).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(marcado("cliente", "cpf_cnpj", CPF)).isZero();
        assertThat(marcado("veiculo", "placa", PLACA)).isZero();
        assertThat(listarClientes(atendente)).hasSize(1);
        assertThat(listarVeiculos(atendente)).hasSize(1);
    }

    private void criarCliente(HttpHeaders headers) {
        ResponseEntity<ClienteResponse> criado = restTemplate.exchange(
                "/api/clientes",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Maria Souza", "cpfCnpj", CPF), headers),
                ClienteResponse.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void criarVeiculo(HttpHeaders headers) {
        ResponseEntity<VeiculoResponse> criado = restTemplate.exchange(
                "/api/veiculos",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "cpfCnpjCliente", CPF,
                        "placa", PLACA,
                        "marca", "Fiat",
                        "modelo", "Uno",
                        "ano", 2012), headers),
                VeiculoResponse.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<String> excluir(HttpHeaders headers, String rota) {
        return restTemplate.exchange(rota, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> buscar(HttpHeaders headers, String rota) {
        return restTemplate.exchange(rota, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ClienteResponse[] listarClientes(HttpHeaders headers) {
        ResponseEntity<ClienteResponse[]> lista = restTemplate.exchange(
                "/api/clientes", HttpMethod.GET, new HttpEntity<>(headers), ClienteResponse[].class);
        assertThat(lista.getBody()).isNotNull();
        return lista.getBody();
    }

    private VeiculoResponse[] listarVeiculos(HttpHeaders headers) {
        ResponseEntity<VeiculoResponse[]> lista = restTemplate.exchange(
                "/api/veiculos", HttpMethod.GET, new HttpEntity<>(headers), VeiculoResponse[].class);
        assertThat(lista.getBody()).isNotNull();
        return lista.getBody();
    }

    private int marcado(String tabela, String coluna, String id) {
        return jdbc.queryForObject(
                "select count(*) from " + tabela + " where " + coluna + " = ? and excluida_em is not null",
                Integer.class,
                id);
    }

    private int linhas(String tabela, String coluna, String id) {
        return jdbc.queryForObject(
                "select count(*) from " + tabela + " where " + coluna + " = ?",
                Integer.class,
                id);
    }

    private HttpHeaders headers(String papel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token(papel));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
