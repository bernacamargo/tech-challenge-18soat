package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Fake do port de pecas para testar os casos de uso sem Spring/JPA. */
final class PecaRepositorioEmMemoria implements PecaRepositorio {

    private final Map<UUID, Peca> pecas = new LinkedHashMap<>();
    private int salvamentos;

    PecaRepositorioEmMemoria com(Peca... existentes) {
        for (Peca peca : existentes) {
            pecas.put(peca.id(), peca);
        }
        return this;
    }

    int salvamentos() {
        return salvamentos;
    }

    @Override
    public Optional<Peca> buscarPorId(UUID id) {
        return Optional.ofNullable(pecas.get(id));
    }

    @Override
    public List<Peca> listar() {
        return new ArrayList<>(pecas.values());
    }

    @Override
    public Peca salvar(Peca peca) {
        salvamentos++;
        pecas.put(peca.id(), peca);
        return peca;
    }

    @Override
    public void remover(UUID id) {
        pecas.remove(id);
    }
}
