package br.com.grupo117.oficina.catalogoestoque.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetario em reais, com duas casas decimais. Nunca negativo; usado em
 * precos do catalogo e na composicao do orcamento.
 */
public record Dinheiro(BigDecimal valor) {

    private static final int CASAS_DECIMAIS = 2;

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    public Dinheiro {
        if (valor == null) {
            throw new DinheiroInvalidoException("Valor monetario e obrigatorio");
        }
        if (valor.signum() < 0) {
            throw new DinheiroInvalidoException("Valor monetario nao pode ser negativo");
        }
        valor = valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public static Dinheiro de(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DinheiroInvalidoException("Valor monetario e obrigatorio");
        }
        try {
            return new Dinheiro(new BigDecimal(valor.trim()));
        } catch (NumberFormatException e) {
            throw new DinheiroInvalidoException("Valor monetario invalido: " + valor);
        }
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro multiplicar(int quantidade) {
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade)));
    }

    public boolean positivo() {
        return valor.signum() > 0;
    }
}
