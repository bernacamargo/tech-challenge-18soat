package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

    protected OrdemDeServicoEntity() {}

    public static OrdemDeServicoEntity de(OrdemDeServico os) {
        OrdemDeServicoEntity entity = new OrdemDeServicoEntity();
        entity.id = os.id();
        entity.clienteId = os.clienteId();
        entity.veiculoId = os.veiculoId();
        entity.status = os.status();
        entity.finalizadaEm = os.finalizadaEm().orElse(null);
        return entity;
    }

    public OrdemDeServico paraDominio() {
        return OrdemDeServico.restaurar(id, clienteId, veiculoId, status, finalizadaEm);
    }
}
