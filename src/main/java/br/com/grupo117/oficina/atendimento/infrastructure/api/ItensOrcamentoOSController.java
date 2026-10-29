package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.infrastructure.service.ItensOrcamentoOSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordens-de-servico")
@Tag(name = "Ordens de serviço")
@SecurityRequirement(name = "bearer-jwt")
@ApiResponses({@ApiResponse(responseCode = "400", description = "Requisição inválida ou item ausente no catálogo"),
        @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "409", description = "Estado, estoque ou reservas incompatíveis; disputa concorrente")})
public class ItensOrcamentoOSController {
    private final ItensOrcamentoOSService service;

    public ItensOrcamentoOSController(ItensOrcamentoOSService service) { this.service = service; }

    @PostMapping("/{id}/servicos")
    @PreAuthorize("hasAnyRole('MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Inclui um serviço na OS", description = "Mecânico consulta o catálogo e copia nome e preço para um novo item. Permitido em Recebida ou Em diagnóstico, antes do orçamento.")
    @ApiResponse(responseCode = "200", description = "OS com serviço incluído")
    public OrdemDeServicoResponse incluirServico(@PathVariable UUID id, @Valid @RequestBody ServicoDaOSRequest request) {
        return OrdemDeServicoResponse.de(service.incluirServico(id, request.servicoId(), request.quantidade()));
    }

    @PostMapping("/{id}/pecas")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Inclui uma peça na OS e reserva o saldo", description = "Copia nome e preço do catálogo. Item e reserva são gravados juntos, antes do orçamento. A reserva reduz o saldo livre. A baixa ocorre quando a execução inicia; a rejeição devolve a quantidade ao livre.")
    @ApiResponse(responseCode = "200", description = "OS com peça incluída e estoque reservado")
    public OrdemDeServicoResponse incluirPeca(@PathVariable UUID id, @Valid @RequestBody PecaDaOSRequest request) {
        return OrdemDeServicoResponse.de(service.incluirPeca(id, request.pecaId(), request.quantidade()));
    }
}
