package br.com.grupo117.oficina.catalogoestoque.application.port;

import br.com.grupo117.oficina.catalogoestoque.domain.ReservaDeEstoque;
import java.util.List;
import java.util.UUID;

/** Port de saida para persistencia das reservas de estoque; o adaptador fica na infraestrutura. */
public interface ReservaDeEstoqueRepositorio {

    ReservaDeEstoque salvar(ReservaDeEstoque reserva);

    List<ReservaDeEstoque> buscarAtivasPorOrdemDeServico(UUID ordemDeServicoId);
}
