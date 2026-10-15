package br.com.grupo117.oficina.atendimento.infrastructure.config;

import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.application.IdentificarCliente;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.usecase.CriarOrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.DetalharOrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.ListarOrdensDeServico;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class OrdemDeServicoConfig {
    @Bean
    CriarOrdemDeServico criarOrdemDeServico(ClienteRepositorio clientes, CadastrarVeiculo cadastrarVeiculo,
                                           VeiculoRepositorio veiculos, OrdemDeServicoRepositorio ordens) {
        return new CriarOrdemDeServico(new IdentificarCliente(clientes), cadastrarVeiculo, veiculos, ordens);
    }

    @Bean
    ListarOrdensDeServico listarOrdensDeServico(OrdemDeServicoRepositorio ordens) {
        return new ListarOrdensDeServico(ordens);
    }

    @Bean
    DetalharOrdemDeServico detalharOrdemDeServico(OrdemDeServicoRepositorio ordens) {
        return new DetalharOrdemDeServico(ordens);
    }
}
