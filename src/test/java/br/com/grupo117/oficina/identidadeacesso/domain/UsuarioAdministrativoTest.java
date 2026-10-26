package br.com.grupo117.oficina.identidadeacesso.domain;

import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.ADMINISTRADOR;
import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.ATENDENTE;
import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.MECANICO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UsuarioAdministrativoTest {

    @Test
    void criaComIdLoginEPerfis() {
        UsuarioAdministrativo u = new UsuarioAdministrativo("sub-1", "atendente", Set.of(ATENDENTE));

        assertEquals("sub-1", u.id());
        assertEquals("atendente", u.login());
        assertEquals(Set.of(ATENDENTE), u.perfis());
    }

    @Test
    void aceitaMaisDeUmPerfil() {
        UsuarioAdministrativo u = new UsuarioAdministrativo("sub-1", "atendente", Set.of(ATENDENTE, ADMINISTRADOR));

        assertEquals(Set.of(ATENDENTE, ADMINISTRADOR), u.perfis());
    }

    @Test
    void perfisSaoCopiaImutavel() {
        Set<Perfil> origem = new HashSet<>(Set.of(ATENDENTE));
        UsuarioAdministrativo u = new UsuarioAdministrativo("sub-1", "atendente", origem);

        origem.add(MECANICO);

        assertEquals(Set.of(ATENDENTE), u.perfis());
        assertThrows(UnsupportedOperationException.class, () -> u.perfis().add(ADMINISTRADOR));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaIdNuloOuEmBranco(String id) {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> new UsuarioAdministrativo(id, "atendente", Set.of(ATENDENTE)));

        assertEquals(CredencialInvalidaException.class, e.getClass());
        assertEquals("Credencial sem identificador", e.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaLoginNuloOuEmBranco(String login) {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> new UsuarioAdministrativo("sub-1", login, Set.of(ATENDENTE)));

        assertEquals(CredencialInvalidaException.class, e.getClass());
        assertEquals("Credencial sem login", e.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void semPerfilDaOficinaFalhaComSubtipo(Set<Perfil> perfis) {
        UsuarioSemPerfilDaOficinaException e = assertThrows(UsuarioSemPerfilDaOficinaException.class,
                () -> new UsuarioAdministrativo("sub-1", "atendente", perfis));

        assertEquals(UsuarioSemPerfilDaOficinaException.class, e.getClass());
        assertEquals("sub-1", e.sujeito());
        assertEquals("Usuario sem perfil da oficina", e.getMessage());
        assertFalse(e.getMessage().contains("sub-1"));
    }

    @Test
    void conjuntoSoComNuloFalhaPorFaltaDePerfil() {
        Set<Perfil> soNulo = new HashSet<>(Collections.singleton(null));

        assertThrows(UsuarioSemPerfilDaOficinaException.class,
                () -> new UsuarioAdministrativo("sub-1", "atendente", soNulo));
    }

    @Test
    void ignoraPerfilNuloNoConjunto() {
        UsuarioAdministrativo u = new UsuarioAdministrativo("sub-1", "atendente",
                new HashSet<>(Arrays.asList(ATENDENTE, null)));

        assertEquals(Set.of(ATENDENTE), u.perfis());
    }

    @Test
    void semPerfilNaoEhCredencialInvalida() {
        assertFalse(CredencialInvalidaException.class.isAssignableFrom(UsuarioSemPerfilDaOficinaException.class));
        assertFalse(IllegalArgumentException.class.isAssignableFrom(UsuarioSemPerfilDaOficinaException.class));
    }

    @Test
    void identificadorAusenteTemPrecedenciaSobrePerfilAusente() {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> new UsuarioAdministrativo(null, "atendente", Set.of()));

        assertEquals(CredencialInvalidaException.class, e.getClass());
        assertEquals("Credencial sem identificador", e.getMessage());
    }

    @Test
    void loginAusenteTemPrecedenciaSobrePerfilAusente() {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> new UsuarioAdministrativo("sub-1", " ", Set.of()));

        assertEquals(CredencialInvalidaException.class, e.getClass());
        assertEquals("Credencial sem login", e.getMessage());
    }

    @Test
    void deDescartaRolesTecnicos() {
        UsuarioAdministrativo u = UsuarioAdministrativo.de(new DadosDaCredencial("sub-1", "atendente",
                Set.of("ATENDENTE", "default-roles-oficina", "offline_access")));

        assertEquals(Set.of(ATENDENTE), u.perfis());
        assertEquals("sub-1", u.id());
        assertEquals("atendente", u.login());
    }

    @Test
    void deSoComRolesTecnicosFalhaPorFaltaDePerfil() {
        DadosDaCredencial dados = new DadosDaCredencial("sub-1", "atendente",
                Set.of("default-roles-oficina", "offline_access", "uma_authorization", "atendente"));

        UsuarioSemPerfilDaOficinaException e = assertThrows(UsuarioSemPerfilDaOficinaException.class,
                () -> UsuarioAdministrativo.de(dados));

        assertEquals("sub-1", e.sujeito());
    }

    @Test
    void deComDadosNulosFalha() {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> UsuarioAdministrativo.de(null));

        assertEquals(CredencialInvalidaException.class, e.getClass());
        assertEquals("Credencial invalida", e.getMessage());
    }

    @Test
    void deComSujeitoAusenteFalhaComoCredencialInvalida() {
        DadosDaCredencial dados = new DadosDaCredencial(null, "atendente", Set.of());

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> UsuarioAdministrativo.de(dados));

        assertEquals(CredencialInvalidaException.class, e.getClass());
    }

    @Test
    void possuiPerfil() {
        UsuarioAdministrativo u = new UsuarioAdministrativo("sub-1", "mecanico", Set.of(MECANICO));

        assertTrue(u.possui(MECANICO));
        assertFalse(u.possui(ADMINISTRADOR));
        assertFalse(u.possui(null));
    }

    @Test
    void igualdadePorValor() {
        UsuarioAdministrativo primeiro = new UsuarioAdministrativo("sub-1", "atendente", Set.of(ATENDENTE));
        UsuarioAdministrativo segundo = new UsuarioAdministrativo("sub-1", "atendente", Set.of(ATENDENTE));

        assertEquals(primeiro, segundo);
        assertEquals(primeiro.hashCode(), segundo.hashCode());
        assertNotEquals(primeiro, new UsuarioAdministrativo("sub-2", "atendente", Set.of(ATENDENTE)));
    }
}
