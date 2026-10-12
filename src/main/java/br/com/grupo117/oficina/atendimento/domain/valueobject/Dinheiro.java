package br.com.grupo117.oficina.atendimento.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinheiro(BigDecimal valor) {
    public Dinheiro {
        Objects.requireNonNull(valor, "Valor monetário é obrigatório");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("Valor monetário não pode ser negativo");
        }
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException erro) {
            throw new IllegalArgumentException("Valor monetário deve ter precisão de centavos", erro);
        }
    }

    public Dinheiro multiplicar(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade)));
    }
}
