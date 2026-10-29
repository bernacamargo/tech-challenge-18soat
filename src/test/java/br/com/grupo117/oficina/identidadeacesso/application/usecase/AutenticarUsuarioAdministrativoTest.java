package br.com.grupo117.oficina.identidadeacesso.application.usecase;

import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.ADMINISTRADOR;
import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.ATENDENTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.grupo117.oficina.identidadeacesso.domain.Credencial;
import br.com.grupo117.oficina.identidadeacesso.domain.CredencialInvalidaException;
import br.com.grupo117.oficina.identidadeacesso.domain.DadosDaCredencial;
import br.com.grupo117.oficina.identidadeacesso.domain.UsuarioAdministrativo;
import br.com.grupo117.oficina.identidadeacesso.domain.UsuarioSemPerfilDaOficinaException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AutenticarUsuarioAdministrativoTest {

    @Test
    void credencialValidaDevolveUsuarioComPerfisDaOficina() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake()
                .aceitando("t1", new DadosDaCredencial("sub-1", "atendente", Set.of("ATENDENTE", "default-roles-oficina")));
        Credencial credencial = new Credencial("t1");

        UsuarioAdministrativo usuario = new AutenticarUsuarioAdministrativo(fake).autenticar(credencial);

        assertEquals("sub-1", usuario.id());
        assertEquals("atendente", usuario.login());
        assertEquals(Set.of(ATENDENTE), usuario.perfis());
        assertEquals(1, fake.chamadas());
        assertSame(credencial, fake.ultima());
    }

    @Test
    void aceitaVariosPerfisDaOficina() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake()
                .aceitando("t2", new DadosDaCredencial("sub-1", "admin", Set.of("uma_authorization", "ADMINISTRADOR", "ATENDENTE")));

        UsuarioAdministrativo usuario = new AutenticarUsuarioAdministrativo(fake).autenticar(new Credencial("t2"));

        assertEquals(Set.of(ADMINISTRADOR, ATENDENTE), usuario.perfis());
    }

    @Test
    void credencialRecusadaPeloValidadorPropagaAMesmaExcecao() {
        CredencialInvalidaException falha = new CredencialInvalidaException("Credencial invalida");
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake().recusando("t3", falha);
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        assertSame(falha, assertThrows(CredencialInvalidaException.class,
                () -> casoDeUso.autenticar(new Credencial("t3"))));
        assertEquals(1, fake.chamadas());
    }

    @Test
    void usuarioSemPerfilDaOficinaFalhaComMotivoProprio() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake()
                .aceitando("t4", new DadosDaCredencial("sub-2", "tecnico", Set.of("default-roles-oficina", "offline_access")));
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        UsuarioSemPerfilDaOficinaException e = assertThrows(UsuarioSemPerfilDaOficinaException.class,
                () -> casoDeUso.autenticar(new Credencial("t4")));

        assertEquals(UsuarioSemPerfilDaOficinaException.class, e.getClass());
        assertEquals("sub-2", e.sujeito());
    }

    @Test
    void perfilEmMinusculasNaoEhPerfilDaOficina() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake()
                .aceitando("t5", new DadosDaCredencial("sub-1", "admin", Set.of("administrador")));
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        assertThrows(UsuarioSemPerfilDaOficinaException.class, () -> casoDeUso.autenticar(new Credencial("t5")));
    }

    @Test
    void credencialNulaFalhaSemConsultarValidador() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake();
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class, () -> casoDeUso.autenticar(null));

        assertEquals("Credencial ausente", e.getMessage());
        assertEquals(0, fake.chamadas());
    }

    @Test
    void validadorQueDevolveNuloNaoAutentica() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake().devolvendoNulo();
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        assertThrows(CredencialInvalidaException.class, () -> casoDeUso.autenticar(new Credencial("t6")));
        assertEquals(1, fake.chamadas());
    }

    @Test
    void dadosSemIdentificadorNaoAutenticam() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake()
                .aceitando("t7", new DadosDaCredencial(" ", "atendente", Set.of("ATENDENTE")));
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> casoDeUso.autenticar(new Credencial("t7")));

        assertEquals("Credencial sem identificador", e.getMessage());
    }

    @Test
    void dadosSemLoginNaoAutenticam() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake()
                .aceitando("t8", new DadosDaCredencial("sub-3", "", Set.of("ATENDENTE")));
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> casoDeUso.autenticar(new Credencial("t8")));

        assertEquals("Credencial sem login", e.getMessage());
    }

    @Test
    void falhaDoProvedorNaoViraCredencialInvalida() {
        IllegalStateException falha = new IllegalStateException("JWKS indisponivel");
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake().recusando("t9", falha);
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> casoDeUso.autenticar(new Credencial("t9")));

        assertSame(falha, e);
        assertEquals(1, fake.chamadas());
    }

    @Test
    void cadaAutenticacaoConsultaOValidadorUmaVez() {
        ValidadorDeCredencialFake fake = new ValidadorDeCredencialFake()
                .aceitando("t1", new DadosDaCredencial("sub-1", "atendente", Set.of("ATENDENTE")));
        AutenticarUsuarioAdministrativo casoDeUso = new AutenticarUsuarioAdministrativo(fake);
        Credencial credencial = new Credencial("t1");

        casoDeUso.autenticar(credencial);
        casoDeUso.autenticar(credencial);

        assertEquals(2, fake.chamadas());
    }

    @Test
    void exigeValidador() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new AutenticarUsuarioAdministrativo(null));

        assertEquals("Validador de credencial e obrigatorio", e.getMessage());
    }
}
