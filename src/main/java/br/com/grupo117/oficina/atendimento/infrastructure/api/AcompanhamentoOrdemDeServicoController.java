package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.infrastructure.service.ItensOrcamentoOSService;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acompanhamento/ordens-de-servico")
@Tag(name = "Acompanhamento da OS", description = "Consulta e decisão do cliente por UUID, sem autenticação")
public class AcompanhamentoOrdemDeServicoController {
    private final OrdemDeServicoService consultas;
    private final ItensOrcamentoOSService decisoes;

    public AcompanhamentoOrdemDeServicoController(OrdemDeServicoService consultas, ItensOrcamentoOSService decisoes) {
        this.consultas = consultas;
        this.decisoes = decisoes;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o andamento da OS sem autenticação",
            description = "Informe o UUID recebido da oficina. Retorna somente status e finalização, sem dados pessoais.")
    public AcompanhamentoOrdemDeServicoResponse consultar(@PathVariable UUID id) {
        return AcompanhamentoOrdemDeServicoResponse.de(consultas.detalhar(id));
    }

    @PostMapping("/{id}/aprovacao")
    @Operation(summary = "Cliente aprova a OS", description = "Libera a execução automaticamente e baixa as reservas. Não recebe corpo. Não exige autenticação.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Execução iniciada"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada"),
            @ApiResponse(responseCode = "409", description = "Aprovação não permitida no estado atual")})
    public AcompanhamentoOrdemDeServicoResponse aprovar(@PathVariable UUID id) {
        return AcompanhamentoOrdemDeServicoResponse.de(decisoes.aprovar(id));
    }

    @PostMapping("/{id}/rejeicao")
    @Operation(summary = "Cliente rejeita a OS", description = "Mantém Aguardando aprovação, registra o motivo e devolve peças e insumos ao estoque. Não exige autenticação.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OS rejeitada e estoque atualizado"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada"),
            @ApiResponse(responseCode = "409", description = "Rejeição não permitida no estado atual")})
    public AcompanhamentoOrdemDeServicoResponse rejeitar(@PathVariable UUID id,
                                                        @Valid @RequestBody RejeicaoOrcamentoRequest request) {
        return AcompanhamentoOrdemDeServicoResponse.de(decisoes.rejeitar(id, request.motivo()));
    }
}
