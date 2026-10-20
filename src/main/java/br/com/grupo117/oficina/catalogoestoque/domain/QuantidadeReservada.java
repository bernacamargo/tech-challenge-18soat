package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Quantidade de uma peca separada para uma ordem de servico. Sempre positiva.
 */
public record QuantidadeReservada(int valor) {

    public QuantidadeReservada {
        if (valor <= 0) {
            throw new QuantidadeInvalidaException("Quantidade deve ser maior que zero");
        }
    }

    public static QuantidadeReservada de(int valor) {
        return new QuantidadeReservada(valor);
    }
}
