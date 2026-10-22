package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PublicadorDeEventos;
import br.com.grupo117.oficina.catalogoestoque.domain.EventoDeDominio;
import java.util.ArrayList;
import java.util.List;

/** Fake do publicador que guarda os eventos recebidos para conferencia nos testes. */
final class PublicadorDeEventosEmMemoria implements PublicadorDeEventos {

    private final List<EventoDeDominio> publicados = new ArrayList<>();

    List<EventoDeDominio> publicados() {
        return publicados;
    }

    @Override
    public void publicar(List<EventoDeDominio> eventos) {
        publicados.addAll(eventos);
    }
}
