package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EmailClienteTest {
    private final ClienteRepositorio clientes = mock(ClienteRepositorio.class);
    private final CpfCnpj doc = CpfCnpj.de("52998224725");

    @Test
    void cadastroEAlteracaoPreservamContatoQuandoOmitido() {
        when(clientes.salvar(any())).thenAnswer(i -> i.getArgument(0));
        var criado = new CadastrarCliente(clientes).cadastrar("Maria", doc.digitos(), "maria@exemplo.com");
        assertThat(criado.email()).contains(new Email("maria@exemplo.com"));
        when(clientes.buscarPorCpfCnpj(doc)).thenReturn(Optional.of(criado));
        var caso = new AlterarCliente(clientes);
        assertThat(caso.alterar(doc.digitos(), "Maria Souza").email()).isEqualTo(criado.email());
        assertThat(caso.alterar(doc.digitos(), "Maria", "novo@exemplo.com").email()).contains(new Email("novo@exemplo.com"));
    }

    @Test
    void identificacaoPreencheApenasContatoAusenteSemSobrescrever() {
        when(clientes.salvar(any())).thenAnswer(i -> i.getArgument(0));
        var caso = new IdentificarCliente(clientes);
        assertThat(caso.identificar(doc.digitos(), "Maria", "a@b.com").email()).contains(new Email("a@b.com"));
        when(clientes.buscarPorCpfCnpj(doc)).thenReturn(Optional.of(new Cliente("Maria", doc)));
        assertThat(caso.identificar(doc.digitos(), "Outro nome", "a@b.com").nome()).isEqualTo("Maria");
        var existente = new Cliente("Maria", doc, new Email("original@b.com"));
        when(clientes.buscarPorCpfCnpj(doc)).thenReturn(Optional.of(existente));
        assertThat(caso.identificar(doc.digitos(), "Outro nome", "novo@b.com")).isSameAs(existente);
        assertThat(caso.identificar(doc.digitos(), "Maria")).isSameAs(existente);
    }
}
