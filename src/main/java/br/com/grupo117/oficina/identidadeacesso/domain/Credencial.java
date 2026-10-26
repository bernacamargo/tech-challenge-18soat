package br.com.grupo117.oficina.identidadeacesso.domain;

/**
 * Credencial apresentada pelo usuario administrativo: o access token Bearer emitido
 * pelo provedor de identidade. O valor e opaco para o dominio (nao e interpretado aqui)
 * e nunca aparece em toString.
 */
public record Credencial(String valor) {

    public Credencial {
        if (valor == null || valor.isBlank()) {
            throw new CredencialInvalidaException("Credencial ausente");
        }
        if (valor.chars().anyMatch(Character::isWhitespace)) {
            throw new CredencialInvalidaException("Credencial invalida");
        }
    }

    public static Credencial de(String valor) {
        return new Credencial(valor);
    }

    @Override
    public String toString() {
        return "Credencial[valor=***]";
    }
}
