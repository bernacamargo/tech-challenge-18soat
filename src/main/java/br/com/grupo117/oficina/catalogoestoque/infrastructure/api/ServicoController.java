package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.application.usecase.AlterarServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.CadastrarServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.DetalharServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.ListarServicos;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.RemoverServico;
import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/servicos")
@Tag(name = "Servicos", description = "Catalogo de servicos da oficina")
@SecurityRequirement(name = "bearer-jwt")
public class ServicoController {

    private final CadastrarServico cadastrarServico;
    private final ListarServicos listarServicos;
    private final DetalharServico detalharServico;
    private final AlterarServico alterarServico;
    private final RemoverServico removerServico;

    public ServicoController(
            CadastrarServico cadastrarServico,
            ListarServicos listarServicos,
            DetalharServico detalharServico,
            AlterarServico alterarServico,
            RemoverServico removerServico
    ) {
        this.cadastrarServico = cadastrarServico;
        this.listarServicos = listarServicos;
        this.detalharServico = detalharServico;
        this.alterarServico = alterarServico;
        this.removerServico = removerServico;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cadastra um servico")
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        Servico servico = cadastrarServico.cadastrar(request.nome(), request.descricao(), request.preco());
        URI location = URI.create("/api/servicos/" + servico.id());
        return ResponseEntity.created(location).body(ServicoResponse.de(servico));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Lista os servicos")
    public List<ServicoResponse> listar() {
        return listarServicos.listar().stream().map(ServicoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Detalha um servico pelo id")
    public ServicoResponse detalhar(@PathVariable UUID id) {
        return ServicoResponse.de(detalharServico.detalhar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Altera um servico")
    public ServicoResponse alterar(@PathVariable UUID id, @Valid @RequestBody ServicoRequest request) {
        return ServicoResponse.de(alterarServico.alterar(id, request.nome(), request.descricao(), request.preco()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Remove um servico")
    public void remover(@PathVariable UUID id) {
        removerServico.remover(id);
    }
}
