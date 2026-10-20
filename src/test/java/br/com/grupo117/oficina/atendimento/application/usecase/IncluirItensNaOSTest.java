package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.*;
import br.com.grupo117.oficina.atendimento.domain.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class IncluirItensNaOSTest {
    private final UUID catalogoId = UUID.randomUUID();
    private final OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
    private final OrdensFake ordens = new OrdensFake(os);
    private final CatalogoFake catalogo = new CatalogoFake(new ItemCatalogo(catalogoId, "Filtro", new BigDecimal("12.75")));
    private final EstoqueFake estoque = new EstoqueFake(3);

    @ParameterizedTest
    @EnumSource(TipoItemOS.class)
    void incluiComPrecoVigenteEPreservaSnapshot(TipoItemOS tipo) {
        OrdemDeServico resultado = incluir(tipo, os.id(), catalogoId, 2);
        assertThat(resultado).isSameAs(os);
        assertThat(os.itens()).hasSize(1);
        ItemDaOS item = os.itens().getFirst();
        assertThat(item.tipo()).isEqualTo(tipo);
        assertThat(item.catalogoId()).isEqualTo(catalogoId);
        assertThat(item.quantidade()).isEqualTo(2);
        assertThat(item.subtotal().valor()).isEqualByComparingTo("25.50");
        assertThat(ordens.salvamentos).isEqualTo(1);
        catalogo.dados = new ItemCatalogo(catalogoId, "Outro nome", new BigDecimal("99.00"));
        assertThat(item.precoUnitario().valor()).isEqualByComparingTo("12.75");
        if (tipo == TipoItemOS.PECA) {
            assertThat(estoque.disponivel).isEqualTo(1);
            assertThat(estoque.osId).isEqualTo(os.id());
            assertThat(estoque.itemId).isEqualTo(item.id());
            assertThat(estoque.pecaId).isEqualTo(catalogoId);
        } else {
            assertThat(estoque.chamadas).isZero();
        }
    }

    @Test
    void semEstoqueNaoIncluiNemSalva() {
        assertThatIllegalStateException().isThrownBy(() -> incluir(TipoItemOS.PECA, os.id(), catalogoId, 4))
                .withMessageContaining("Estoque insuficiente");
        assertThat(os.itens()).isEmpty();
        assertThat(ordens.salvamentos).isZero();
        assertThat(estoque.disponivel).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(TipoItemOS.class)
    void osAusenteNaoConsultaCatalogoOuReserva(TipoItemOS tipo) {
        UUID ausente = UUID.randomUUID();
        assertThatThrownBy(() -> incluir(tipo, ausente, catalogoId, 1))
                .isInstanceOf(OrdemDeServicoNaoEncontradaException.class);
        assertSemEfeitos();
        assertThat(catalogo.consultas).isZero();
    }

    @ParameterizedTest
    @EnumSource(TipoItemOS.class)
    void catalogoAusenteNaoInclui(TipoItemOS tipo) {
        catalogo.dados = null;
        assertThatIllegalArgumentException().isThrownBy(() -> incluir(tipo, os.id(), catalogoId, 1));
        assertSemEfeitos();
    }

    @ParameterizedTest
    @EnumSource(TipoItemOS.class)
    void quantidadeInvalidaOuIdNuloNaoTemEfeitos(TipoItemOS tipo) {
        assertThatIllegalArgumentException().isThrownBy(() -> incluir(tipo, os.id(), catalogoId, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> incluir(tipo, os.id(), catalogoId, -1));
        assertThatNullPointerException().isThrownBy(() -> incluir(tipo, null, catalogoId, 1));
        assertThatNullPointerException().isThrownBy(() -> incluir(tipo, os.id(), null, 1));
        assertSemEfeitos();
        assertThat(catalogo.consultas).isZero();
    }

    @ParameterizedTest
    @EnumSource(TipoItemOS.class)
    void estadoBloqueadoNaoReserva(TipoItemOS tipo) {
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.transicionarPara(StatusOS.AGUARDANDO_APROVACAO);
        assertThatIllegalStateException().isThrownBy(() -> incluir(tipo, os.id(), catalogoId, 1));
        assertSemEfeitos();
        assertThat(catalogo.consultas).isZero();
    }

    @ParameterizedTest
    @EnumSource(TipoItemOS.class)
    void validaDadosDeIntegracaoAntesDeReservar(TipoItemOS tipo) {
        catalogo.dados = new ItemCatalogo(UUID.randomUUID(), "Peça", BigDecimal.TEN);
        assertThatIllegalArgumentException().isThrownBy(() -> incluir(tipo, os.id(), catalogoId, 1));
        catalogo.dados = new ItemCatalogo(catalogoId, "Peça", BigDecimal.ZERO);
        assertThatIllegalArgumentException().isThrownBy(() -> incluir(tipo, os.id(), catalogoId, 1));
        catalogo.dados = new ItemCatalogo(catalogoId, " ", BigDecimal.TEN);
        assertThatIllegalArgumentException().isThrownBy(() -> incluir(tipo, os.id(), catalogoId, 1));
        assertSemEfeitos();
    }

    @Test
    void falhaDeReservaNaoIncluiNemSalva() {
        ReservaEstoquePort falho = (ordem, item, peca, quantidade) -> { throw new IllegalStateException("Falha no estoque"); };
        IncluirPecaNaOS casoDeUso = new IncluirPecaNaOS(ordens, catalogo, falho);
        assertThatIllegalStateException().isThrownBy(() -> casoDeUso.incluir(os.id(), catalogoId, 1))
                .withMessage("Falha no estoque");
        assertThat(os.itens()).isEmpty();
        assertThat(ordens.salvamentos).isZero();
    }

    private OrdemDeServico incluir(TipoItemOS tipo, UUID osId, UUID itemId, int quantidade) {
        return tipo == TipoItemOS.SERVICO
                ? new IncluirServicoNaOS(ordens, catalogo).incluir(osId, itemId, quantidade)
                : new IncluirPecaNaOS(ordens, catalogo, estoque).incluir(osId, itemId, quantidade);
    }

    private void assertSemEfeitos() {
        assertThat(os.itens()).isEmpty();
        assertThat(ordens.salvamentos).isZero();
        assertThat(estoque.chamadas).isZero();
    }

    private static final class OrdensFake implements OrdemDeServicoRepositorio {
        private final OrdemDeServico os;
        private int salvamentos;
        OrdensFake(OrdemDeServico os) { this.os = os; }
        @Override
        public Optional<OrdemDeServico> buscarPorId(UUID id) { return Optional.of(os).filter(o -> o.id().equals(id)); }
        @Override
        public List<OrdemDeServico> listar() { return List.of(os); }
        @Override
        public OrdemDeServico salvar(OrdemDeServico os) { salvamentos++; return os; }
    }

    private static final class CatalogoFake implements CatalogoOSPort {
        private ItemCatalogo dados;
        private int consultas;
        CatalogoFake(ItemCatalogo dados) { this.dados = dados; }
        @Override
        public Optional<ItemCatalogo> buscarServico(UUID id) { consultas++; return Optional.ofNullable(dados); }
        @Override
        public Optional<ItemCatalogo> buscarPeca(UUID id) { consultas++; return Optional.ofNullable(dados); }
    }

    private static final class EstoqueFake implements ReservaEstoquePort {
        private int disponivel;
        private int chamadas;
        private UUID osId;
        private UUID itemId;
        private UUID pecaId;
        EstoqueFake(int disponivel) { this.disponivel = disponivel; }
        @Override
        public boolean reservar(UUID osId, UUID itemId, UUID pecaId, int quantidade) {
            chamadas++;
            if (quantidade > disponivel) return false;
            disponivel -= quantidade;
            this.osId = osId;
            this.itemId = itemId;
            this.pecaId = pecaId;
            return true;
        }
    }
}
