package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.Placa;

/** Informa se cliente ou veículo ainda sustentam outro cadastro ativo ou uma OS. */
public interface VinculoDeCadastro {

    static VinculoDeCadastro nenhum() {
        return new VinculoDeCadastro() {
            @Override
            public boolean clienteEmUso(CpfCnpj cpfCnpj) {
                return false;
            }

            @Override
            public boolean veiculoEmUso(Placa placa) {
                return false;
            }
        };
    }

    boolean clienteEmUso(CpfCnpj cpfCnpj);

    boolean veiculoEmUso(Placa placa);
}
