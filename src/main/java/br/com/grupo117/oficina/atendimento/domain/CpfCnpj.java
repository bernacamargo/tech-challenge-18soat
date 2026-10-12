package br.com.grupo117.oficina.atendimento.domain;

/**
 * Identificacao fiscal do cliente. Aceita CPF (11 digitos) ou CNPJ (14 digitos),
 * com ou sem mascara, e so existe se os digitos verificadores conferem.
 */
public record CpfCnpj(String digitos) {

    private static final int TAMANHO_CPF = 11;
    private static final int TAMANHO_CNPJ = 14;
    private static final int MODULO = 11;
    private static final int LIMITE_RESTO_ZERO = 2;

    private static final int[] PESOS_CPF_PRIMEIRO = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CPF_SEGUNDO = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_PRIMEIRO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_SEGUNDO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public CpfCnpj {
        String normalizado = extrairDigitos(digitos);
        if (!digitosVerificadoresConferem(normalizado)) {
            throw new CpfCnpjInvalidoException("CPF/CNPJ invalido");
        }
        digitos = normalizado;
    }

    public static CpfCnpj de(String valor) {
        return new CpfCnpj(valor);
    }

    public boolean cpf() {
        return digitos.length() == TAMANHO_CPF;
    }

    public boolean cnpj() {
        return digitos.length() == TAMANHO_CNPJ;
    }

    private static String extrairDigitos(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new CpfCnpjInvalidoException("CPF/CNPJ e obrigatorio");
        }
        StringBuilder somenteDigitos = new StringBuilder(valor.length());
        for (int indice = 0; indice < valor.length(); indice++) {
            char caractere = valor.charAt(indice);
            if (caractere >= '0' && caractere <= '9') {
                somenteDigitos.append(caractere);
            } else if (!separadorPermitido(caractere)) {
                throw new CpfCnpjInvalidoException("CPF/CNPJ invalido");
            }
        }
        return somenteDigitos.toString();
    }

    private static boolean separadorPermitido(char caractere) {
        return caractere == '.' || caractere == '-' || caractere == '/' || Character.isWhitespace(caractere);
    }

    private static boolean digitosVerificadoresConferem(String digitos) {
        if (digitos.length() == TAMANHO_CPF && !todosIguais(digitos)) {
            return confere(digitos, PESOS_CPF_PRIMEIRO, PESOS_CPF_SEGUNDO);
        }
        if (digitos.length() == TAMANHO_CNPJ && !todosIguais(digitos)) {
            return confere(digitos, PESOS_CNPJ_PRIMEIRO, PESOS_CNPJ_SEGUNDO);
        }
        return false;
    }

    private static boolean todosIguais(String digitos) {
        char primeiro = digitos.charAt(0);
        for (int indice = 1; indice < digitos.length(); indice++) {
            if (digitos.charAt(indice) != primeiro) {
                return false;
            }
        }
        return true;
    }

    private static boolean confere(String digitos, int[] pesosPrimeiro, int[] pesosSegundo) {
        int primeiro = calcularDigito(digitos, pesosPrimeiro);
        int segundo = calcularDigito(digitos, pesosSegundo);
        return digitoNaPosicao(digitos, pesosPrimeiro.length) == primeiro
                && digitoNaPosicao(digitos, pesosSegundo.length) == segundo;
    }

    private static int calcularDigito(String digitos, int[] pesos) {
        int soma = 0;
        for (int indice = 0; indice < pesos.length; indice++) {
            soma += (digitos.charAt(indice) - '0') * pesos[indice];
        }
        int resto = soma % MODULO;
        if (resto < LIMITE_RESTO_ZERO) {
            return 0;
        }
        return MODULO - resto;
    }

    private static int digitoNaPosicao(String digitos, int posicao) {
        return digitos.charAt(posicao) - '0';
    }
}
