package br.com.grupo117.oficina.catalogoestoque.infrastructure;

import br.com.grupo117.oficina.catalogoestoque.application.AlterarServico;
import br.com.grupo117.oficina.catalogoestoque.application.CadastrarServico;
import br.com.grupo117.oficina.catalogoestoque.application.DetalharServico;
import br.com.grupo117.oficina.catalogoestoque.application.ListarServicos;
import br.com.grupo117.oficina.catalogoestoque.application.RemoverServico;
import br.com.grupo117.oficina.catalogoestoque.application.ServicoRepositorio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga os casos de uso de servico ao port, sem anotar a camada de aplicacao com Spring.
 */
@Configuration
public class ServicoUseCaseConfig {

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
