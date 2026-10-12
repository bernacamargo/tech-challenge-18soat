package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.AlterarVeiculo;
import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.DetalharVeiculo;
import br.com.grupo117.oficina.atendimento.application.ListarVeiculos;
import br.com.grupo117.oficina.atendimento.application.RemoverVeiculo;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veiculos")
@Tag(name = "Veiculos", description = "Cadastro de veiculos da oficina")
@SecurityRequirement(name = "bearer-jwt")
public class VeiculoController {

    private final CadastrarVeiculo cadastrarVeiculo;
    private final ListarVeiculos listarVeiculos;
    private final DetalharVeiculo detalharVeiculo;
    private final AlterarVeiculo alterarVeiculo;
    private final RemoverVeiculo removerVeiculo;

    public VeiculoController(
            CadastrarVeiculo cadastrarVeiculo,
            ListarVeiculos listarVeiculos,
            DetalharVeiculo detalharVeiculo,
            AlterarVeiculo alterarVeiculo,
            RemoverVeiculo removerVeiculo
    ) {
        this.cadastrarVeiculo = cadastrarVeiculo;
        this.listarVeiculos = listarVeiculos;
        this.detalharVeiculo = detalharVeiculo;
        this.alterarVeiculo = alterarVeiculo;
        this.removerVeiculo = removerVeiculo;
    }

    @PostMapping
    @Operation(summary = "Cadastra um veiculo ligado a um cliente")
    public ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request) {
        Veiculo veiculo = cadastrarVeiculo.cadastrar(
                request.cpfCnpjCliente(),
                request.placa(),
                request.marca(),
                request.modelo(),
                request.ano()
        );
        URI location = URI.create("/api/veiculos/" + veiculo.placa().valor());
        return ResponseEntity.created(location).body(VeiculoResponse.de(veiculo));
    }

    @GetMapping
    @Operation(summary = "Lista os veiculos")
    public List<VeiculoResponse> listar() {
        return listarVeiculos.listar().stream().map(VeiculoResponse::de).toList();
    }

    @GetMapping("/{placa}")
    @Operation(summary = "Detalha um veiculo pela placa")
    public VeiculoResponse detalhar(@PathVariable String placa) {
        return VeiculoResponse.de(detalharVeiculo.detalhar(placa));
    }

    @PutMapping("/{placa}")
    @Operation(summary = "Altera marca, modelo e ano de um veiculo")
    public VeiculoResponse alterar(@PathVariable String placa, @Valid @RequestBody AlterarVeiculoRequest request) {
        return VeiculoResponse.de(alterarVeiculo.alterar(placa, request.marca(), request.modelo(), request.ano()));
    }

    @DeleteMapping("/{placa}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um veiculo")
    public void remover(@PathVariable String placa) {
        removerVeiculo.remover(placa);
    }
}
