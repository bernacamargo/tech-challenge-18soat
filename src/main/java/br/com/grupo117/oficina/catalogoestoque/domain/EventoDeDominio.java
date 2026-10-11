package br.com.grupo117.oficina.catalogoestoque.domain;

import java.time.Instant;

/**
 * Fato relevante ocorrido no estoque. Os agregados acumulam os eventos e a camada
 * de aplicacao os publica apos persistir.
 */
public interface EventoDeDominio {

    Instant ocorridoEm();
}
