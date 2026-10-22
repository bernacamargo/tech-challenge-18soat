package br.com.grupo117.oficina.atendimento.infrastructure.config;

import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.application.IdentificarCliente;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.usecase.CriarOrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.DetalharOrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.ListarOrdensDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.IniciarDiagnostico;
import br.com.grupo117.oficina.atendimento.application.usecase.ConcluirServicos;
import br.com.grupo117.oficina.atendimento.application.usecase.RegistrarEntrega;
import br.com.grupo117.oficina.atendimento.application.usecase.GerarOrcamento;
import java.time.Clock;
import java.time.Duration;
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
    @Bean
    IniciarDiagnostico iniciarDiagnostico(OrdemDeServicoRepositorio ordens) {
        return new IniciarDiagnostico(ordens);
    }

    @Bean
    ConcluirServicos concluirServicos(OrdemDeServicoRepositorio ordens) {
        // PostgreSQL armazena timestamps com precisão de microssegundos.
        return new ConcluirServicos(ordens, Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000)));
    }

    @Bean
    RegistrarEntrega registrarEntrega(OrdemDeServicoRepositorio ordens) {
        return new RegistrarEntrega(ordens);
    }
    @Bean
    GerarOrcamento gerarOrcamento(OrdemDeServicoRepositorio ordens) {
        return new GerarOrcamento(ordens);
    }
}
