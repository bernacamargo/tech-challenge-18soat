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
        @ApiResponse(responseCode = "403", description = "Papel sem permissão"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada"),
        @ApiResponse(responseCode = "409", description = "Estado, estoque ou reservas incompatíveis; disputa concorrente")})
public class ItensOrcamentoOSController {
    private final ItensOrcamentoOSService service;

    public ItensOrcamentoOSController(ItensOrcamentoOSService service) { this.service = service; }

    @PostMapping("/{id}/servicos")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Inclui um serviço na OS", description = "Copia nome e preço do catálogo para um novo item. Permitido em Recebida ou Em diagnóstico, antes do orçamento.")
    @ApiResponse(responseCode = "200", description = "OS com serviço incluído")
    public OrdemDeServicoResponse incluirServico(@PathVariable UUID id, @Valid @RequestBody ServicoDaOSRequest request) {
        return OrdemDeServicoResponse.de(service.incluirServico(id, request.servicoId(), request.quantidade()));
    }

    @PostMapping("/{id}/pecas")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Inclui uma peça na OS e reserva o saldo", description = "Copia nome e preço do catálogo. Item e reserva são gravados juntos, antes do orçamento. A reserva reduz o saldo livre; a baixa física ocorre na aprovação.")
    @ApiResponse(responseCode = "200", description = "OS com peça incluída e estoque reservado")
    public OrdemDeServicoResponse incluirPeca(@PathVariable UUID id, @Valid @RequestBody PecaDaOSRequest request) {
        return OrdemDeServicoResponse.de(service.incluirPeca(id, request.pecaId(), request.quantidade()));
    }

    @PostMapping("/{id}/aprovacao")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Registra a aprovação do cliente e inicia a execução", description = "Atendente ou administrador registra o consentimento do cliente. Baixa as reservas e altera Aguardando aprovação para Em execução com horário do servidor, em uma única transação. Não recebe corpo.")
    @ApiResponse(responseCode = "200", description = "Orçamento aprovado e execução iniciada")
    public OrdemDeServicoResponse aprovar(@PathVariable UUID id) {
        return OrdemDeServicoResponse.de(service.aprovar(id));
    }
}
