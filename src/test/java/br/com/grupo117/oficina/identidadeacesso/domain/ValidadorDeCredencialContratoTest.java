package br.com.grupo117.oficina.identidadeacesso.domain;

import static br.com.grupo117.oficina.identidadeacesso.domain.Perfil.ATENDENTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ValidadorDeCredencialContratoTest {

    @Test
    void credencialConhecidaProduzUsuarioAdministrativo() {
        ValidadorDeCredencialEmMemoria fake = new ValidadorDeCredencialEmMemoria()
                .com("token-atendente", new DadosDaCredencial("sub-1", "atendente", Set.of("ATENDENTE", "offline_access")));

        UsuarioAdministrativo u = UsuarioAdministrativo.de(fake.validar(new Credencial("token-atendente")));

        assertEquals(new UsuarioAdministrativo("sub-1", "atendente", Set.of(ATENDENTE)), u);
        assertEquals(1, fake.chamadas());
    }

    @Test
    void credencialDesconhecidaFalhaComCredencialInvalida() {
        ValidadorDeCredencialEmMemoria fake = new ValidadorDeCredencialEmMemoria();

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> fake.validar(new Credencial("token-desconhecido")));

        assertEquals(CredencialInvalidaException.class, e.getClass());
        assertEquals(1, fake.chamadas());
    }

    @Test
    void credencialEmBrancoFalhaAntesDeChamarOValidador() {
        ValidadorDeCredencialEmMemoria fake = new ValidadorDeCredencialEmMemoria()
                .com("token-atendente", new DadosDaCredencial("sub-1", "atendente", Set.of("ATENDENTE")));

        assertThrows(CredencialInvalidaException.class, () -> fake.validar(new Credencial(" ")));

        assertEquals(0, fake.chamadas());
    }

    @Test
    void credencialConhecidaSemPerfilDaOficinaFalhaComSubtipo() {
        ValidadorDeCredencialEmMemoria fake = new ValidadorDeCredencialEmMemoria()
                .com("token-tecnico", new DadosDaCredencial("sub-9", "tecnico", Set.of("offline_access")));

        UsuarioSemPerfilDaOficinaException e = assertThrows(UsuarioSemPerfilDaOficinaException.class,
                () -> UsuarioAdministrativo.de(fake.validar(new Credencial("token-tecnico"))));

        assertEquals("sub-9", e.sujeito());
    }

    @Test
    void falhaDoProvedorPropagaSemVirarCredencialInvalida() {
        ValidadorDeCredencialEmMemoria fake = new ValidadorDeCredencialEmMemoria()
                .falhandoCom(new IllegalStateException("Provedor indisponivel"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> fake.validar(new Credencial("token-atendente")));

        assertEquals(IllegalStateException.class, e.getClass());
    }

    private static final class ValidadorDeCredencialEmMemoria implements ValidadorDeCredencial {

        private final Map<String, DadosDaCredencial> credenciais = new HashMap<>();
        private RuntimeException falhaDoProvedor;
        private int chamadas;

        ValidadorDeCredencialEmMemoria com(String valor, DadosDaCredencial dados) {
            credenciais.put(valor, dados);
            return this;
        }

        ValidadorDeCredencialEmMemoria falhandoCom(RuntimeException falha) {
            this.falhaDoProvedor = falha;
            return this;
        }

        int chamadas() {
            return chamadas;
        }

        @Override
        public DadosDaCredencial validar(Credencial credencial) {
            chamadas++;
            if (falhaDoProvedor != null) {
                throw falhaDoProvedor;
            }
            DadosDaCredencial dados = credenciais.get(credencial.valor());
            if (dados == null) {
                throw new CredencialInvalidaException("Credencial invalida");
            }
            return dados;
        }
    }
}
