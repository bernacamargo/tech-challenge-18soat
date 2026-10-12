package br.com.grupo117.oficina.atendimento.domain;

import java.time.Year;
import java.util.Objects;

/**
 * Veiculo atendido na oficina. Identificado pela placa e pertencente a um cliente.
 */
public final class Veiculo {

    private static final int ANO_MINIMO = 1900;

    private final Cliente cliente;
    private final Placa placa;
    private final String marca;
    private final String modelo;
    private final int ano;

    public Veiculo(Cliente cliente, Placa placa, String marca, String modelo, int ano) {
        this.cliente = Objects.requireNonNull(cliente, "Cliente do veiculo e obrigatorio");
        this.placa = Objects.requireNonNull(placa, "Placa e obrigatoria");
        this.marca = textoObrigatorio(marca, "Marca do veiculo e obrigatoria");
        this.modelo = textoObrigatorio(modelo, "Modelo do veiculo e obrigatorio");
        this.ano = anoValido(ano);
    }

    public static Veiculo cadastrar(Cliente cliente, String placa, String marca, String modelo, int ano) {
        return new Veiculo(cliente, Placa.de(placa), marca, modelo, ano);
    }

    public Cliente cliente() {
        return cliente;
    }

    public Placa placa() {
        return placa;
    }

    public String marca() {
        return marca;
    }

    public String modelo() {
        return modelo;
    }

    public int ano() {
        return ano;
    }

    private static String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private static int anoValido(int ano) {
        int anoMaximo = Year.now().getValue() + 1;
        if (ano < ANO_MINIMO || ano > anoMaximo) {
            throw new IllegalArgumentException("Ano do veiculo invalido");
        }
        return ano;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Veiculo veiculo)) {
            return false;
        }
        return placa.equals(veiculo.placa);
    }

    @Override
    public int hashCode() {
        return placa.hashCode();
    }

    @Override
    public String toString() {
        return "Veiculo[placa=" + placa.valor()
                + ", marca=" + marca
                + ", modelo=" + modelo
                + ", ano=" + ano
                + ", cliente=" + cliente.cpfCnpj().digitos()
                + "]";
    }
}
