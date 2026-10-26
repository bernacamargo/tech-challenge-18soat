package br.com.grupo117.oficina.identidadeacesso.domain;

import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.ADMINISTRADOR;
import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.ATENDENTE;
import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.MECANICO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PerfilTest {

    @Test
    void existemExatamenteOsTresPerfisDoAdr0006() {
        assertEquals(Set.of(ATENDENTE, MECANICO, ADMINISTRADOR), EnumSet.allOf(Perfil.class));
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void reconheceOsTresPerfisPeloNomeExato(Perfil perfil) {
        assertEquals(Optional.of(perfil), Perfil.deNome(perfil.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"default-roles-oficina", "offline_access", "uma_authorization"})
    void ignoraRolesTecnicosDoKeycloak(String nome) {
        assertEquals(Optional.empty(), Perfil.deNome(nome));
    }

    @ParameterizedTest
    @ValueSource(strings = {"atendente", " ATENDENTE", "ATENDENTE ", "Administrador", "mecanico", "ROLE_ADMINISTRADOR"})
    void naoNormalizaCaixaNemEspacos(String nome) {
        assertEquals(Optional.empty(), Perfil.deNome(nome));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "OUTRO", "CLIENTE"})
    void ignoraNuloVazioEDesconhecido(String nome) {
        assertEquals(Optional.empty(), assertDoesNotThrow(() -> Perfil.deNome(nome)));
    }

    @Test
    void reconhecerFiltraEDeduplica() {
        assertEquals(Set.of(ATENDENTE, MECANICO),
                Perfil.reconhecer(List.of("ATENDENTE", "offline_access", "ATENDENTE", "MECANICO")));
    }

    @Test
    void reconhecerAceitaOsTresPerfis() {
        assertEquals(EnumSet.allOf(Perfil.class),
                Perfil.reconhecer(List.of("ATENDENTE", "MECANICO", "ADMINISTRADOR")));
    }

    @Test
    void reconhecerIgnoraElementoNulo() {
        assertEquals(Set.of(ATENDENTE), Perfil.reconhecer(Arrays.asList("ATENDENTE", null)));
    }

    @Test
    void reconhecerComColecaoNulaDevolveVazio() {
        assertEquals(Set.of(), Perfil.reconhecer(null));
    }

    @Test
    void reconhecerDescartaTecnicosECaixaDiferente() {
        assertEquals(Set.of(), Perfil.reconhecer(List.of(
                "default-roles-oficina", "offline_access", "uma_authorization", "atendente", "ROLE_MECANICO")));
    }

    @Test
    void conjuntoReconhecidoEhImutavel() {
        Set<Perfil> perfis = Perfil.reconhecer(List.of("ATENDENTE"));

        assertThrows(UnsupportedOperationException.class, () -> perfis.add(MECANICO));
    }
}
