package br.com.grupo117.oficina.catalogoestoque.infrastructure.config;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.application.port.PublicadorDeEventos;
import br.com.grupo117.oficina.catalogoestoque.application.port.ReservaDeEstoqueRepositorio;
import br.com.grupo117.oficina.catalogoestoque.application.port.ServicoRepositorio;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.AlterarPeca;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.AlterarServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.CadastrarPeca;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.CadastrarServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.DarEntradaNoEstoque;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.DetalharPeca;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.DetalharServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.ListarPecas;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.ListarServicos;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.MovimentarEstoqueDaOrdemDeServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.RemoverPeca;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.RemoverServico;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogoEstoqueConfig {
    @Bean
    CadastrarPeca cadastrarPeca(PecaRepositorio pecas) {
        return new CadastrarPeca(pecas);
    }

    @Bean
    ListarPecas listarPecas(PecaRepositorio pecas) {
        return new ListarPecas(pecas);
    }

    @Bean
    DetalharPeca detalharPeca(PecaRepositorio pecas) {
        return new DetalharPeca(pecas);
    }

    @Bean
    AlterarPeca alterarPeca(PecaRepositorio pecas) {
        return new AlterarPeca(pecas);
    }

    @Bean
    RemoverPeca removerPeca(PecaRepositorio pecas) {
        return new RemoverPeca(pecas);
    }

    @Bean
    DarEntradaNoEstoque darEntradaNoEstoque(PecaRepositorio pecas, PublicadorDeEventos eventos) {
        return new DarEntradaNoEstoque(pecas, eventos);
    }

    @Bean
    MovimentarEstoqueDaOrdemDeServico movimentarEstoqueDaOrdemDeServico(
            PecaRepositorio pecas, ReservaDeEstoqueRepositorio reservas, PublicadorDeEventos eventos) {
        return new MovimentarEstoqueDaOrdemDeServico(pecas, reservas, eventos);
    }

    @Bean
    CadastrarServico cadastrarServico(ServicoRepositorio servicos) {
        return new CadastrarServico(servicos);
    }

    @Bean
    ListarServicos listarServicos(ServicoRepositorio servicos) {
        return new ListarServicos(servicos);
    }

    @Bean
    DetalharServico detalharServico(ServicoRepositorio servicos) {
        return new DetalharServico(servicos);
    }

    @Bean
    AlterarServico alterarServico(ServicoRepositorio servicos) {
        return new AlterarServico(servicos);
    }

    @Bean
    RemoverServico removerServico(ServicoRepositorio servicos) {
        return new RemoverServico(servicos);
    }
}
