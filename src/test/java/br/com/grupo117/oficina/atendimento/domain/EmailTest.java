package br.com.grupo117.oficina.atendimento.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class EmailTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "a", "a@", "@b.com", "a@b", "a@@b.com", "a b@c.com", "a@b..com", "a@b.com\r\nBcc:x@y.com"})
    void rejeitaContatoInvalido(String valor) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Email(valor));
    }

    @Test
    void validaTamanhoEPreservaCaixaDoEndereco() {
        assertThat(new Email("Maria+oficina@exemplo.com").valor()).isEqualTo("Maria+oficina@exemplo.com");
        assertThatIllegalArgumentException().isThrownBy(() -> new Email("a".repeat(250) + "@b.com"));
        assertThat(new Email("a".repeat(248) + "@b.com").valor()).hasSize(254);
        Cliente legado = Cliente.cadastrar("Maria", "52998224725");
        assertThat(legado.email()).isEmpty();
        assertThat(new Cliente("Maria", legado.cpfCnpj(), new Email("a@b.com")).email())
                .contains(new Email("a@b.com"));
    }
}
