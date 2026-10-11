package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.AlterarCliente;
import br.com.grupo117.oficina.atendimento.application.CadastrarCliente;
import br.com.grupo117.oficina.atendimento.application.DetalharCliente;
import br.com.grupo117.oficina.atendimento.application.ListarClientes;
import br.com.grupo117.oficina.atendimento.application.RemoverCliente;
import br.com.grupo117.oficina.atendimento.domain.Cliente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Cadastro de clientes da oficina")
@SecurityRequirement(name = "bearer-jwt")
public class ClienteController {

    private final CadastrarCliente cadastrarCliente;
    private final ListarClientes listarClientes;
    private final DetalharCliente detalharCliente;
    private final AlterarCliente alterarCliente;
    private final RemoverCliente removerCliente;

    public ClienteController(
            CadastrarCliente cadastrarCliente,
            ListarClientes listarClientes,
            DetalharCliente detalharCliente,
            AlterarCliente alterarCliente,
            RemoverCliente removerCliente
    ) {
        this.cadastrarCliente = cadastrarCliente;
        this.listarClientes = listarClientes;
        this.detalharCliente = detalharCliente;
        this.alterarCliente = alterarCliente;
        this.removerCliente = removerCliente;
    }

    @PostMapping
    @Operation(summary = "Cadastra um cliente")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        Cliente cliente = cadastrarCliente.cadastrar(request.nome(), request.cpfCnpj());
        URI location = URI.create("/api/clientes/" + cliente.cpfCnpj().digitos());
        return ResponseEntity.created(location).body(ClienteResponse.de(cliente));
    }

    @GetMapping
    @Operation(summary = "Lista os clientes")
    public List<ClienteResponse> listar() {
        return listarClientes.listar().stream().map(ClienteResponse::de).toList();
    }

    @GetMapping("/{cpfCnpj}")
    @Operation(summary = "Detalha um cliente pelo CPF/CNPJ")
    public ClienteResponse detalhar(@PathVariable String cpfCnpj) {
        return ClienteResponse.de(detalharCliente.detalhar(cpfCnpj));
    }

    @PutMapping("/{cpfCnpj}")
    @Operation(summary = "Altera o nome de um cliente")
    public ClienteResponse alterar(@PathVariable String cpfCnpj, @Valid @RequestBody AlterarClienteRequest request) {
        return ClienteResponse.de(alterarCliente.alterar(cpfCnpj, request.nome()));
    }

    @DeleteMapping("/{cpfCnpj}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um cliente")
    public void remover(@PathVariable String cpfCnpj) {
        removerCliente.remover(cpfCnpj);
    }
}
