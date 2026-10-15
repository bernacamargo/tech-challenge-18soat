package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.service.PecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/pecas")
@Tag(name = "Pecas e insumos", description = "Catalogo de pecas e insumos com controle de estoque")
@SecurityRequirement(name = "bearer-jwt")
public class PecaController {
    private final PecaService service;

    public PecaController(PecaService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cadastra uma peca ou insumo", description = "Cria o item com a quantidade inicial em estoque")
    public ResponseEntity<PecaResponse> cadastrar(@Valid @RequestBody PecaRequest request) {
        Peca peca = service.cadastrar(request.nome(), request.descricao(), request.tipo(), request.precoUnitario(),
                request.quantidadeInicial());
        return ResponseEntity.created(URI.create("/api/pecas/" + peca.id())).body(PecaResponse.de(peca));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Lista pecas e insumos", description = "Inclui quantidades disponivel, reservada e livre")
    public List<PecaResponse> listar() {
        return service.listar().stream().map(PecaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Detalha uma peca ou insumo")
    public PecaResponse detalhar(@PathVariable UUID id) {
        return PecaResponse.de(service.detalhar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Altera os dados cadastrais",
            description = "Nao altera quantidades; use entradas, reservas e baixas")
    public PecaResponse alterar(@PathVariable UUID id, @Valid @RequestBody AlterarPecaRequest request) {
        return PecaResponse.de(service.alterar(id, request.nome(), request.descricao(), request.tipo(),
                request.precoUnitario()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Remove uma peca ou insumo")
    public void remover(@PathVariable UUID id) {
        service.remover(id);
    }

    @PostMapping("/{id}/entradas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Da entrada no estoque", description = "Soma a quantidade ao disponivel (EstoqueReabastecido)")
    public PecaResponse darEntrada(@PathVariable UUID id, @Valid @RequestBody EntradaNoEstoqueRequest request) {
        return PecaResponse.de(service.darEntrada(id, request.quantidade()));
    }
}
