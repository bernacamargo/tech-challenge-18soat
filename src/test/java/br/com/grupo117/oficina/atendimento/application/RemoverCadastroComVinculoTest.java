package br.com.grupo117.oficina.atendimento.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoverCadastroComVinculoTest {

    @Mock
    private ClienteRepositorio clientes;

    @Mock
    private VeiculoRepositorio veiculos;

    @Mock
    private VinculoDeCadastro vinculos;

    @Test
    void clienteEmUsoNaoEMarcadoComoExcluido() {
        when(clientes.buscarPorCpfCnpj(CpfCnpj.de("52998224725")))
                .thenReturn(Optional.of(Cliente.cadastrar("Maria", "52998224725")));
        when(vinculos.clienteEmUso(CpfCnpj.de("52998224725"))).thenReturn(true);

        CadastroComVinculoException erro = assertThrows(
                CadastroComVinculoException.class,
                () -> new RemoverCliente(clientes, vinculos).remover("52998224725"));

        assertEquals(
                "Cadastro possui vinculos com outros registros e nao pode ser removido ou alterado",
                erro.getMessage());
        verify(clientes, never()).remover(CpfCnpj.de("52998224725"));
    }

    @Test
    void clienteSemVinculoEMarcadoComoExcluido() {
        when(clientes.buscarPorCpfCnpj(CpfCnpj.de("52998224725")))
                .thenReturn(Optional.of(Cliente.cadastrar("Maria", "52998224725")));
        when(vinculos.clienteEmUso(CpfCnpj.de("52998224725"))).thenReturn(false);

        new RemoverCliente(clientes, vinculos).remover("52998224725");

        verify(clientes).remover(CpfCnpj.de("52998224725"));
    }

    @Test
    void veiculoEmUsoNaoEMarcadoComoExcluido() {
        Cliente maria = Cliente.cadastrar("Maria", "52998224725");
        when(veiculos.buscarPorPlaca(Placa.de("ABC1D23")))
                .thenReturn(Optional.of(Veiculo.cadastrar(maria, "ABC1D23", "Fiat", "Uno", 2012)));
        when(vinculos.veiculoEmUso(Placa.de("ABC1D23"))).thenReturn(true);

        assertThrows(
                CadastroComVinculoException.class,
                () -> new RemoverVeiculo(veiculos, vinculos).remover("ABC1D23"));

        verify(veiculos, never()).remover(Placa.de("ABC1D23"));
    }

    @Test
    void veiculoSemVinculoEMarcadoComoExcluido() {
        Cliente maria = Cliente.cadastrar("Maria", "52998224725");
        when(veiculos.buscarPorPlaca(Placa.de("ABC1D23")))
                .thenReturn(Optional.of(Veiculo.cadastrar(maria, "ABC1D23", "Fiat", "Uno", 2012)));
        when(vinculos.veiculoEmUso(Placa.de("ABC1D23"))).thenReturn(false);

        new RemoverVeiculo(veiculos, vinculos).remover("ABC1D23");

        verify(veiculos).remover(Placa.de("ABC1D23"));
    }
}
