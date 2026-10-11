package br.com.grupo117.oficina.atendimento.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpjInvalidoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClienteCrudTest {

    private ClienteRepositorioFake repositorio;
    private CadastrarCliente cadastrar;
    private ListarClientes listar;
    private DetalharCliente detalhar;
    private AlterarCliente alterar;
    private RemoverCliente remover;

    @BeforeEach
    void setUp() {
        repositorio = new ClienteRepositorioFake();
        cadastrar = new CadastrarCliente(repositorio);
        listar = new ListarClientes(repositorio);
        detalhar = new DetalharCliente(repositorio);
        alterar = new AlterarCliente(repositorio);
        remover = new RemoverCliente(repositorio);
    }

    @Test
    void cadastraListaDetalhaAlteraERemove() {
        Cliente maria = cadastrar.cadastrar("  Maria Souza  ", "529.982.247-25");

        assertEquals("Maria Souza", maria.nome());
        assertEquals("52998224725", detalhar.detalhar("52998224725").cpfCnpj().digitos());
        assertEquals(List.of(maria), listar.listar());

        Cliente alterada = alterar.alterar("529.982.247-25", "Maria S. Souza");
        assertEquals("Maria S. Souza", alterada.nome());
        assertEquals("Maria S. Souza", detalhar.detalhar("52998224725").nome());

        remover.remover("52998224725");
        assertTrue(listar.listar().isEmpty());
    }

    @Test
    void documentoRepetidoNaoCadastraOutroCliente() {
        cadastrar.cadastrar("Maria Souza", "52998224725");

        ClienteJaCadastradoException erro = assertThrows(
                ClienteJaCadastradoException.class,
                () -> cadastrar.cadastrar("Outra Pessoa", "529.982.247-25")
        );

        assertEquals("Cliente ja cadastrado: 52998224725", erro.getMessage());
        assertEquals(1, listar.listar().size());
    }

    @Test
    void clienteInexistenteNaoEDetalhadoAlteradoNemRemovido() {
        assertThrows(ClienteNaoEncontradoException.class, () -> detalhar.detalhar("52998224725"));
        assertThrows(ClienteNaoEncontradoException.class, () -> alterar.alterar("52998224725", "Maria"));
        assertThrows(ClienteNaoEncontradoException.class, () -> remover.remover("52998224725"));
        assertTrue(repositorio.vazio());
    }

    @Test
    void documentoInvalidoNaoAlteraORepositorio() {
        assertThrows(CpfCnpjInvalidoException.class, () -> cadastrar.cadastrar("Maria", "111.111.111-11"));
        assertThrows(CpfCnpjInvalidoException.class, () -> detalhar.detalhar("111.111.111-11"));
        assertTrue(repositorio.vazio());
    }

    private static final class ClienteRepositorioFake implements ClienteRepositorio {

        private final Map<CpfCnpj, Cliente> clientes = new HashMap<>();

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
            clientes.put(cliente.cpfCnpj(), cliente);
            return cliente;
        }

        @Override
        public void remover(CpfCnpj cpfCnpj) {
            clientes.remove(cpfCnpj);
        }

        boolean vazio() {
            return clientes.isEmpty();
        }
    }
}
