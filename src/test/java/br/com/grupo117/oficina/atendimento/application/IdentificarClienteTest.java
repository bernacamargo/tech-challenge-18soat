package br.com.grupo117.oficina.atendimento.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpjInvalidoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdentificarClienteTest {

    @Test
    void documentoNovoCadastraOCliente() {
        ClienteRepositorioFake repositorio = new ClienteRepositorioFake();
        IdentificarCliente casoDeUso = new IdentificarCliente(repositorio);

        Cliente cliente = casoDeUso.identificar("529.982.247-25", "  Maria Souza  ");

        assertEquals("Maria Souza", cliente.nome());
        assertEquals(CpfCnpj.de("52998224725"), cliente.cpfCnpj());
        assertEquals(1, repositorio.quantidade());
        assertSame(cliente, repositorio.buscarPorCpfCnpj(cliente.cpfCnpj()).orElseThrow());
    }

    @Test
    void documentoExistenteIdentificaOClienteJaSalvo() {
        ClienteRepositorioFake repositorio = new ClienteRepositorioFake();
        Cliente salvo = Cliente.cadastrar("Maria Souza", "52998224725");
        repositorio.salvar(salvo);
        IdentificarCliente casoDeUso = new IdentificarCliente(repositorio);

        Cliente identificado = casoDeUso.identificar("529.982.247-25", "Outro Nome");

        assertSame(salvo, identificado);
        assertEquals("Maria Souza", identificado.nome());
        assertEquals(1, repositorio.quantidade());
        assertEquals(1, repositorio.salvamentos());
    }

    @Test
    void documentoInvalidoNaoCadastraCliente() {
        ClienteRepositorioFake repositorio = new ClienteRepositorioFake();
        IdentificarCliente casoDeUso = new IdentificarCliente(repositorio);

        assertThrows(CpfCnpjInvalidoException.class, () -> casoDeUso.identificar("111.111.111-11", "Maria Souza"));
        assertTrue(repositorio.vazio());
    }

    @Test
    void documentoNovoSemNomeNaoCadastraCliente() {
        ClienteRepositorioFake repositorio = new ClienteRepositorioFake();
        IdentificarCliente casoDeUso = new IdentificarCliente(repositorio);

        assertThrows(IllegalArgumentException.class, () -> casoDeUso.identificar("52998224725", "   "));
        assertTrue(repositorio.vazio());
    }

    private static final class ClienteRepositorioFake implements ClienteRepositorio {

        private final Map<CpfCnpj, Cliente> clientes = new HashMap<>();
        private int salvamentos;

        @Override
        public Optional<Cliente> buscarPorCpfCnpj(CpfCnpj cpfCnpj) {
            return Optional.ofNullable(clientes.get(cpfCnpj));
        }

        @Override
        public List<Cliente> listar() {
            return List.copyOf(clientes.values());
        }

        @Override
        public Cliente salvar(Cliente cliente) {
            salvamentos++;
            clientes.put(cliente.cpfCnpj(), cliente);
            return cliente;
        }

        @Override
        public void remover(CpfCnpj cpfCnpj) {
            clientes.remove(cpfCnpj);
        }

        int quantidade() {
            return clientes.size();
        }

        int salvamentos() {
            return salvamentos;
        }

        boolean vazio() {
            return clientes.isEmpty();
        }
    }
}
