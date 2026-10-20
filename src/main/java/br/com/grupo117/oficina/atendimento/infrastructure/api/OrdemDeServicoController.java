package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Lista as ordens de serviço", description = "Retorna identificação, itens, orçamento, status e finalização")
    public List<OrdemDeServicoResponse> listar() {
        return service.listar().stream().map(OrdemDeServicoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Detalha uma OS", description = "Inclui itens com preços registrados e orçamento quando gerado")
    public OrdemDeServicoResponse detalhar(@PathVariable UUID id) {
        return OrdemDeServicoResponse.de(service.detalhar(id));
    }
}
