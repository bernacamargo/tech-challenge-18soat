package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Orcamento;

@Entity
@Table(name = "ordem_de_servico")
public class OrdemDeServicoEntity {
    @Id
    private UUID id;
    @Column(name = "cpf_cnpj_cliente", nullable = false, length = 14)
    private String clienteId;
    @Column(name = "placa_veiculo", nullable = false, length = 7)
    private String veiculoId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;
    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    @ElementCollection
    @CollectionTable(name = "item_da_os", joinColumns = @JoinColumn(name = "ordem_de_servico_id"))
    @OrderColumn(name = "posicao")
    private List<ItemDaOSEmbeddable> itens = new ArrayList<>();
    @Column(name = "orcamento_total", precision = 19, scale = 2)
    private BigDecimal orcamentoTotal;

    protected OrdemDeServicoEntity() {}

    public static OrdemDeServicoEntity de(OrdemDeServico os) {
        OrdemDeServicoEntity entity = new OrdemDeServicoEntity();
        entity.id = os.id();
        entity.clienteId = os.clienteId();
        entity.veiculoId = os.veiculoId();
        entity.status = os.status();
        entity.finalizadaEm = os.finalizadaEm().orElse(null);
        entity.itens = new ArrayList<>(os.itens().stream().map(ItemDaOSEmbeddable::de).toList());
        entity.orcamentoTotal = os.orcamento().map(orcamento -> orcamento.total().valor()).orElse(null);
        return entity;
    }

    public OrdemDeServico paraDominio() {
        List<ItemDaOS> restaurados = itens.stream().map(ItemDaOSEmbeddable::paraDominio).toList();
        Orcamento orcamento = orcamentoTotal == null ? null : new Orcamento(restaurados);
        if (orcamento != null && orcamento.total().valor().compareTo(orcamentoTotal) != 0) {
            throw new IllegalStateException("Total do orçamento persistido diverge dos itens da OS");
        }
        return OrdemDeServico.restaurar(id, clienteId, veiculoId, status, finalizadaEm, restaurados, orcamento);
    }
}
