package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.AlterarCliente;
import br.com.grupo117.oficina.atendimento.application.CadastrarCliente;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.application.DetalharCliente;
import br.com.grupo117.oficina.atendimento.application.ListarClientes;
import br.com.grupo117.oficina.atendimento.application.RemoverCliente;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga os casos de uso de cliente ao port, sem anotar a camada de aplicacao com Spring.
 */
@Configuration
public class ClienteUseCaseConfig {

    @Bean
    CadastrarCliente cadastrarCliente(ClienteRepositorio clientes) {
        return new CadastrarCliente(clientes);
    }

    @Bean
    ListarClientes listarClientes(ClienteRepositorio clientes) {
        return new ListarClientes(clientes);
    }

    @Bean
    DetalharCliente detalharCliente(ClienteRepositorio clientes) {
        return new DetalharCliente(clientes);
    }

    @Bean
    AlterarCliente alterarCliente(ClienteRepositorio clientes) {
        return new AlterarCliente(clientes);
    }

    @Bean
    RemoverCliente removerCliente(ClienteRepositorio clientes) {
        return new RemoverCliente(clientes);
    }
}
