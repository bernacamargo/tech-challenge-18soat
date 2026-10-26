package br.com.grupo117.oficina.identidadeacesso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DadosDaCredencialTest {

    @Test
    void perfisNulosViramConjuntoVazio() {
        DadosDaCredencial dados = new DadosDaCredencial("sub-1", "atendente", null);

        assertEquals(Set.of(), dados.perfisInformados());
    }

    @Test
    void ignoraPerfilNulo() {
        DadosDaCredencial dados = new DadosDaCredencial("sub-1", "atendente",
                new HashSet<>(Arrays.asList("ATENDENTE", null)));

        assertEquals(Set.of("ATENDENTE"), dados.perfisInformados());
    }

    @Test
    void perfisSaoCopiaImutavel() {
        Set<String> origem = new HashSet<>(Set.of("ATENDENTE"));
        DadosDaCredencial dados = new DadosDaCredencial("sub-1", "atendente", origem);

        origem.add("MECANICO");

        assertEquals(Set.of("ATENDENTE"), dados.perfisInformados());
        assertThrows(UnsupportedOperationException.class, () -> dados.perfisInformados().add("X"));
    }

    @Test
    void preservaPerfisCrusSemFiltrar() {
        DadosDaCredencial dados = new DadosDaCredencial("sub-1", "atendente",
                Set.of("ATENDENTE", "offline_access", "atendente", "ROLE_MECANICO"));

        assertEquals(Set.of("ATENDENTE", "offline_access", "atendente", "ROLE_MECANICO"), dados.perfisInformados());
    }

    @Test
    void naoValidaSujeitoNemLogin() {
        DadosDaCredencial dados = new DadosDaCredencial(null, "", Set.of());

        assertNull(dados.sujeito());
        assertEquals("", dados.login());
    }

    @Test
    void naoTemCamposDePessoa() {
        List<String> componentes = Arrays.stream(DadosDaCredencial.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("sujeito", "login", "perfisInformados"), componentes);
    }

    @Test
    void portTemUmUnicoMetodoValidar() {
        Method[] metodos = ValidadorDeCredencial.class.getDeclaredMethods();

        assertEquals(1, metodos.length);
        Method validar = metodos[0];
        assertEquals("validar", validar.getName());
        assertEquals(List.of(Credencial.class), List.of(validar.getParameterTypes()));
        assertEquals(DadosDaCredencial.class, validar.getReturnType());
        assertTrue(Modifier.isAbstract(validar.getModifiers()));
    }
}
