package br.com.grupo117.oficina.catalogoestoque.infrastructure.evento;

import br.com.grupo117.oficina.catalogoestoque.application.port.PublicadorDeEventos;
import br.com.grupo117.oficina.catalogoestoque.domain.EventoDeDominio;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publica os eventos do estoque como eventos de aplicacao do Spring. */
@Component
public class PublicadorDeEventosSpring implements PublicadorDeEventos {
    private final ApplicationEventPublisher publisher;

    public PublicadorDeEventosSpring(ApplicationEventPublisher publisher) { this.publisher = publisher; }

    @Override
    public void publicar(List<EventoDeDominio> eventos) {
        eventos.forEach(publisher::publishEvent);
    }
}
