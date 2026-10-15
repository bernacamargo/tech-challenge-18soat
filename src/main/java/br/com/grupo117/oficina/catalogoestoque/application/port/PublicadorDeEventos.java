package br.com.grupo117.oficina.catalogoestoque.application.port;

import br.com.grupo117.oficina.catalogoestoque.domain.EventoDeDominio;
import java.util.List;

/** Port de saida para publicar os eventos de dominio do estoque apos a persistencia. */
public interface PublicadorDeEventos {

    void publicar(List<EventoDeDominio> eventos);
}
