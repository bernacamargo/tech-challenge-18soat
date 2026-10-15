package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.application.port.EstoqueDaOrdemDeServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estoque")
@Tag(name = "Estoque da OS", description = "Reserva de pecas para a OS e baixa ao iniciar a execucao")
@SecurityRequirement(name = "bearer-jwt")
public class EstoqueDaOrdemDeServicoController {
    private final EstoqueDaOrdemDeServico estoque;

    public EstoqueDaOrdemDeServicoController(EstoqueDaOrdemDeServico estoque) { this.estoque = estoque; }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Reserva uma peca para a OS",
            description = "Separa a quantidade do estoque livre sem retira-la do disponivel (PecaReservada)")
    public ReservaResponse reservar(@Valid @RequestBody ReservaRequest request) {
        UUID reservaId = estoque.reservar(request.ordemDeServicoId(), request.pecaId(), request.quantidade());
        return new ReservaResponse(reservaId);
    }

    @PostMapping("/ordens-de-servico/{ordemDeServicoId}/baixa")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'MECANICO', 'ADMINISTRADOR')")
    @Operation(summary = "Baixa as reservas da OS ao iniciar a execucao",
            description = "Consome as reservas ativas (EstoqueBaixado); repetir a chamada nao desconta duas vezes")
    public BaixaResponse baixar(@PathVariable UUID ordemDeServicoId) {
        return new BaixaResponse(estoque.baixarReservasDaOrdemDeServico(ordemDeServicoId));
    }
}
