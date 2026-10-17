package br.com.grupo117.oficina.catalogoestoque.application;

import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saida do repositorio de servicos. A implementacao (JPA ou outra)
 * fica na infraestrutura; o caso de uso so depende desta interface.
 */
public interface ServicoRepositorio {

    Optional<Servico> buscarPorId(UUID id);

    List<Servico> listar();

    Servico salvar(Servico servico);

    void remover(UUID id);
}
