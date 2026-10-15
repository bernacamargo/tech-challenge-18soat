package br.com.grupo117.oficina.atendimento.application.port;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.Optional;
import java.util.UUID;

/** Port de saída para persistência da OS; o adaptador será implementado na infraestrutura. */
public interface OrdemDeServicoRepositorio {

    Optional<OrdemDeServico> buscarPorId(UUID id);

    OrdemDeServico salvar(OrdemDeServico ordemDeServico);
}
