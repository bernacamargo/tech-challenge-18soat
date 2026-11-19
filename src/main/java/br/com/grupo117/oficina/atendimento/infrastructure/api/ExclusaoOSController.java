package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.infrastructure.service.ExclusaoOSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordens-de-servico")
@Tag(name = "Ordens de serviço")
@SecurityRequirement(name = "bearer-jwt")
public class ExclusaoOSController {
    private final ExclusaoOSService service;

    public ExclusaoOSController(ExclusaoOSService service) {
        this.service = service;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Exclui logicamente uma OS",
            description = "Exclusivo de ADMINISTRADOR. Oculta a OS das consultas e dos comandos, "
                    + "libera reservas ativas e cancela notificações pendentes.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "OS excluída"),
            @ApiResponse(responseCode = "400", description = "UUID inválido"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão de exclusão"),
            @ApiResponse(responseCode = "404", description = "OS inexistente ou já excluída"),
            @ApiResponse(responseCode = "409", description = "Conflito de concorrência")})
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
