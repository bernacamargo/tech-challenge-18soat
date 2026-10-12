package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.AlterarVeiculo;
import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.application.DetalharVeiculo;
import br.com.grupo117.oficina.atendimento.application.ListarVeiculos;
import br.com.grupo117.oficina.atendimento.application.RemoverVeiculo;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga os casos de uso de veiculo aos ports, sem anotar a camada de aplicacao com Spring.
 */
@Configuration
public class VeiculoUseCaseConfig {

    @Bean
    CadastrarVeiculo cadastrarVeiculo(ClienteRepositorio clientes, VeiculoRepositorio veiculos) {
        return new CadastrarVeiculo(clientes, veiculos);
    }

    @Bean
    ListarVeiculos listarVeiculos(VeiculoRepositorio veiculos) {
        return new ListarVeiculos(veiculos);
    }

    @Bean
    DetalharVeiculo detalharVeiculo(VeiculoRepositorio veiculos) {
        return new DetalharVeiculo(veiculos);
    }

    @Bean
    AlterarVeiculo alterarVeiculo(VeiculoRepositorio veiculos) {
        return new AlterarVeiculo(veiculos);
    }

    @Bean
    RemoverVeiculo removerVeiculo(VeiculoRepositorio veiculos) {
        return new RemoverVeiculo(veiculos);
    }
}
