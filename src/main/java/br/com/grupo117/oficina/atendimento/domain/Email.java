package br.com.grupo117.oficina.atendimento.domain;

/** Contato para notificações. Validação sintática; não comprova existência da caixa postal. */
public record Email(String valor) {
    public Email {
        if (valor == null || valor.isBlank() || valor.length() > 254
                || !valor.matches("[^\\s@<>]+@[^\\s@<>.]+(?:\\.[^\\s@<>.]+)+")) {
            throw new IllegalArgumentException("E-mail inválido");
        }
    }
}
