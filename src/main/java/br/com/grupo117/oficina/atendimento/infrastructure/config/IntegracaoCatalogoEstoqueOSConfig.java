package br.com.grupo117.oficina.atendimento.infrastructure.config;

import br.com.grupo117.oficina.atendimento.application.port.BaixaEstoquePort;
import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort;
import br.com.grupo117.oficina.atendimento.application.usecase.AprovarOrcamento;
import br.com.grupo117.oficina.atendimento.application.usecase.IncluirPecaNaOS;
import br.com.grupo117.oficina.atendimento.application.usecase.IncluirServicoNaOS;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegracaoCatalogoEstoqueOSConfig {
    @Bean
    IncluirServicoNaOS incluirServicoNaOS(OrdemDeServicoRepositorio ordens, CatalogoOSPort catalogo) {
        return new IncluirServicoNaOS(ordens, catalogo);
    }

    @Bean
    IncluirPecaNaOS incluirPecaNaOS(OrdemDeServicoRepositorio ordens, CatalogoOSPort catalogo, ReservaEstoquePort estoque) {
        return new IncluirPecaNaOS(ordens, catalogo, estoque);
    }

    @Bean
    AprovarOrcamento aprovarOrcamento(OrdemDeServicoRepositorio ordens, BaixaEstoquePort estoque) {
        return new AprovarOrcamento(ordens, estoque, Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000)));
    }
}
