package br.com.grupo117.oficina.catalogoestoque.application.port;

import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de saida para persistencia de pecas e insumos; o adaptador fica na infraestrutura. */
public interface PecaRepositorio {

    Optional<Peca> buscarPorId(UUID id);

    List<Peca> listar();

    Peca salvar(Peca peca);

    void remover(UUID id);
}
