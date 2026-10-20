package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordens-de-servico")
@Tag(name = "Ordens de serviço", description = "OS com itens, orçamento e andamento")
@SecurityRequirement(name = "bearer-jwt")
public class OrdemDeServicoController {
    private final OrdemDeServicoService service;

    public OrdemDeServicoController(OrdemDeServicoService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Abre uma OS em Recebida", description = "Cadastra cliente/veículo ausentes em uma única transação")
    public ResponseEntity<OrdemDeServicoResponse> criar(@Valid @RequestBody OrdemDeServicoRequest request) {
        OrdemDeServico os = service.criar(request.documento(), request.nome(), request.placa(),
                request.marca(), request.modelo(), request.ano());
        return ResponseEntity.created(URI.create("/api/ordens-de-servico/" + os.id()))
                .body(OrdemDeServicoResponse.de(os));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Lista as ordens de serviço", description = "Retorna identificação, itens, orçamento, status, início, finalização e rejeição quando registrada")
    public List<OrdemDeServicoResponse> listar() {
        return service.listar().stream().map(OrdemDeServicoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Detalha uma OS", description = "Inclui itens, orçamento, início da execução e data/motivo da rejeição quando registrados")
    public OrdemDeServicoResponse detalhar(@PathVariable UUID id) {
        return OrdemDeServicoResponse.de(service.detalhar(id));
    }

    @PostMapping("/{id}/diagnostico")
    @PreAuthorize("hasAnyRole('MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Inicia o diagnóstico", description = "Altera Recebida para Em diagnóstico. Não recebe corpo.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OS atualizada"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada"),
            @ApiResponse(responseCode = "409", description = "Transição não permitida no estado atual")})
    public OrdemDeServicoResponse iniciarDiagnostico(@PathVariable UUID id) {
        return OrdemDeServicoResponse.de(service.iniciarDiagnostico(id));
    }

    @PostMapping("/{id}/conclusao")
    @PreAuthorize("hasAnyRole('MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Conclui os serviços", description = "Altera Em execução para Finalizada e registra o horário do servidor. Não recebe corpo.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OS finalizada"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada"),
            @ApiResponse(responseCode = "409", description = "Transição não permitida no estado atual")})
    public OrdemDeServicoResponse concluirServicos(@PathVariable UUID id) {
        return OrdemDeServicoResponse.de(service.concluirServicos(id));
    }

    @PostMapping("/{id}/entrega")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Registra a entrega", description = "Altera Finalizada para Entregue e preserva o horário da conclusão. Não recebe corpo.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Entrega registrada"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada"),
            @ApiResponse(responseCode = "409", description = "Transição não permitida no estado atual")})
    public OrdemDeServicoResponse registrarEntrega(@PathVariable UUID id) {
        return OrdemDeServicoResponse.de(service.registrarEntrega(id));
    }

    @PostMapping("/{id}/orcamento")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Gera o orçamento da OS", description = "Soma os itens registrados e altera Em diagnóstico para Aguardando aprovação. Não recebe corpo nem envia e-mail.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OS com orçamento gerado"),
            @ApiResponse(responseCode = "400", description = "OS sem itens ou identificador inválido"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada"),
            @ApiResponse(responseCode = "409", description = "Geração não permitida no estado atual")})
    public OrdemDeServicoResponse gerarOrcamento(@PathVariable UUID id) {
        return OrdemDeServicoResponse.de(service.gerarOrcamento(id));
    }
}
