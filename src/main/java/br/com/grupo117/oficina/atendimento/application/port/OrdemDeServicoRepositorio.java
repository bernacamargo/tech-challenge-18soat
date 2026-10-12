package br.com.grupo117.oficina.atendimento.application.port;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;

/** Port de saída para persistência da OS; o adaptador será implementado na infraestrutura. */
public interface OrdemDeServicoRepositorio {

    OrdemDeServico salvar(OrdemDeServico ordemDeServico);
}
