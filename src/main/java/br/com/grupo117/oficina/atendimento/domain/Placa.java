package br.com.grupo117.oficina.atendimento.domain;

import java.util.Locale;

/**
 * Placa do veiculo. Aceita o formato antigo (ABC1234) e o Mercosul (ABC1D23),
 * com ou sem hifen, e guarda o valor normalizado em maiusculas.
 */
public record Placa(String valor) {

    private static final int TAMANHO = 7;
    private static final int POSICAO_HIFEN = 3;

    public Placa {
        String normalizada = normalizar(valor);
        if (!formatoAntigo(normalizada) && !formatoMercosul(normalizada)) {
            throw new PlacaInvalidaException("Placa invalida");
        }
        valor = normalizada;
    }

    public static Placa de(String valor) {
        return new Placa(valor);
    }

    public boolean antiga() {
        return formatoAntigo(valor);
    }

    public boolean mercosul() {
        return formatoMercosul(valor);
    }

    private static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new PlacaInvalidaException("Placa e obrigatoria");
        }
        String semEspacos = removerEspacos(valor.trim().toUpperCase(Locale.ROOT));
        String semHifen = removerHifenPadrao(semEspacos);
        if (semHifen.length() != TAMANHO || !somenteLetrasEDigitos(semHifen)) {
            throw new PlacaInvalidaException("Placa invalida");
        }
        return semHifen;
    }

    private static String removerEspacos(String valor) {
        StringBuilder semEspacos = new StringBuilder(valor.length());
        for (int indice = 0; indice < valor.length(); indice++) {
            char caractere = valor.charAt(indice);
            if (!Character.isWhitespace(caractere)) {
                semEspacos.append(caractere);
            }
        }
        return semEspacos.toString();
    }

    private static String removerHifenPadrao(String valor) {
        int hifen = valor.indexOf('-');
        if (hifen < 0) {
            return valor;
        }
        if (hifen != POSICAO_HIFEN || valor.indexOf('-', hifen + 1) >= 0) {
            throw new PlacaInvalidaException("Placa invalida");
        }
        return valor.substring(0, hifen) + valor.substring(hifen + 1);
    }

    private static boolean somenteLetrasEDigitos(String valor) {
        for (int indice = 0; indice < valor.length(); indice++) {
            char caractere = valor.charAt(indice);
            if (!letra(caractere) && !digito(caractere)) {
                return false;
            }
        }
        return true;
    }

    private static boolean formatoAntigo(String valor) {
        return letra(valor.charAt(0))
                && letra(valor.charAt(1))
                && letra(valor.charAt(2))
                && digito(valor.charAt(3))
                && digito(valor.charAt(4))
                && digito(valor.charAt(5))
                && digito(valor.charAt(6));
    }

    private static boolean formatoMercosul(String valor) {
        return letra(valor.charAt(0))
                && letra(valor.charAt(1))
                && letra(valor.charAt(2))
                && digito(valor.charAt(3))
                && letra(valor.charAt(4))
                && digito(valor.charAt(5))
                && digito(valor.charAt(6));
    }

    private static boolean letra(char caractere) {
        return caractere >= 'A' && caractere <= 'Z';
    }

    private static boolean digito(char caractere) {
        return caractere >= '0' && caractere <= '9';
    }
}
