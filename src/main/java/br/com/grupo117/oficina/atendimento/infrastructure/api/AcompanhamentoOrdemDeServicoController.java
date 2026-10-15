package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acompanhamento/ordens-de-servico")
@Tag(name = "Acompanhamento da OS", description = "Consulta pública do andamento por UUID, sem autenticação")
public class AcompanhamentoOrdemDeServicoController {
    private final OrdemDeServicoService service;

    public AcompanhamentoOrdemDeServicoController(OrdemDeServicoService service) { this.service = service; }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o andamento da OS sem autenticação",
            description = "Informe o UUID recebido da oficina. Retorna somente status e finalização, sem dados pessoais.")
    public AcompanhamentoOrdemDeServicoResponse consultar(@PathVariable UUID id) {
        return AcompanhamentoOrdemDeServicoResponse.de(service.detalhar(id));
    }
}
