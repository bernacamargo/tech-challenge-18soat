package br.com.grupo117.oficina.catalogoestoque.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetario do catalogo. Guarda duas casas decimais e so aceita quantia positiva.
 */
public record Dinheiro(BigDecimal valor) {

    private static final int ESCALA = 2;

    public Dinheiro {
        if (valor == null) {
            throw new DinheiroInvalidoException("Preco e obrigatorio");
        }
        valor = valor.setScale(ESCALA, RoundingMode.HALF_UP);
        if (valor.signum() <= 0) {
            throw new DinheiroInvalidoException("Preco deve ser maior que zero");
        }
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public static Dinheiro de(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DinheiroInvalidoException("Preco e obrigatorio");
        }
        try {
            return new Dinheiro(new BigDecimal(valor.trim()));
        } catch (NumberFormatException erro) {
            throw new DinheiroInvalidoException("Preco invalido");
        }
    }
}
