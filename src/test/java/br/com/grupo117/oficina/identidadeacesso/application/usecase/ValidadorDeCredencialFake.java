package br.com.grupo117.oficina.identidadeacesso.application.usecase;

import br.com.grupo117.oficina.identidadeacesso.domain.Credencial;
import br.com.grupo117.oficina.identidadeacesso.domain.CredencialInvalidaException;
import br.com.grupo117.oficina.identidadeacesso.domain.DadosDaCredencial;
import br.com.grupo117.oficina.identidadeacesso.domain.ValidadorDeCredencial;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fake do port ValidadorDeCredencial: so procura no mapa e conta chamadas, sem regra de dominio. */
final class ValidadorDeCredencialFake implements ValidadorDeCredencial {

    private final Map<String, DadosDaCredencial> aceitas = new LinkedHashMap<>();
    private final Map<String, RuntimeException> recusadas = new HashMap<>();
    private boolean devolveNulo;
    private int chamadas;
    private Credencial ultima;

    ValidadorDeCredencialFake aceitando(String valor, DadosDaCredencial dados) {
        aceitas.put(valor, dados);
        return this;
    }

    ValidadorDeCredencialFake recusando(String valor, RuntimeException falha) {
        recusadas.put(valor, falha);
        return this;
    }

    ValidadorDeCredencialFake devolvendoNulo() {
        devolveNulo = true;
        return this;
    }

    int chamadas() {
        return chamadas;
    }

    Credencial ultima() {
        return ultima;
    }

    @Override
    public DadosDaCredencial validar(Credencial credencial) {
        chamadas++;
        ultima = credencial;
        if (devolveNulo) {
            return null;
        }
        String valor = credencial.valor();
        if (recusadas.containsKey(valor)) {
            throw recusadas.get(valor);
        }
        if (aceitas.containsKey(valor)) {
            return aceitas.get(valor);
        }
        throw new CredencialInvalidaException("Credencial invalida");
    }
}
