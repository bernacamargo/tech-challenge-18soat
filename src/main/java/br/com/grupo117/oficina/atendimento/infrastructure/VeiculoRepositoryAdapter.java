package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.ClienteNaoEncontradoException;
import br.com.grupo117.oficina.atendimento.application.VeiculoJaCadastradoException;
import br.com.grupo117.oficina.atendimento.application.VeiculoNaoEncontradoException;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA do port de veiculos. O cliente dono e carregado pelo vinculo da tabela.
 */
@Component
@Transactional
public class VeiculoRepositoryAdapter implements VeiculoRepositorio {

    private final VeiculoJpaRepository veiculos;
    private final ClienteJpaRepository clientes;

    public VeiculoRepositoryAdapter(VeiculoJpaRepository veiculos, ClienteJpaRepository clientes) {
        this.veiculos = veiculos;
        this.clientes = clientes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Veiculo> buscarPorPlaca(Placa placa) {
        return veiculos.buscarComCliente(placa.valor()).map(this::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Veiculo> listar() {
        return veiculos.listarComCliente().stream().map(this::paraDominio).toList();
    }

    @Override
    public Veiculo salvar(Veiculo veiculo) {
        String placa = veiculo.placa().valor();
        ClienteEntity cliente = clientes.findById(veiculo.cliente().cpfCnpj().digitos())
                .filter(cadastro -> cadastro.getExcluidaEm() == null)
                .orElseThrow(() -> new ClienteNaoEncontradoException(veiculo.cliente().cpfCnpj()));
        VeiculoEntity entity = veiculos.findById(placa).orElseGet(VeiculoEntity::new);
        if (entity.getExcluidaEm() != null) {
            throw new VeiculoJaCadastradoException(veiculo.placa());
        }
        entity.setPlaca(placa);
        entity.setMarca(veiculo.marca());
        entity.setModelo(veiculo.modelo());
        entity.setAno(veiculo.ano());
        entity.setCliente(cliente);
        return paraDominio(veiculos.save(entity));
    }

    @Override
    public void remover(Placa placa) {
        VeiculoEntity entity = veiculos.findById(placa.valor())
                .filter(cadastro -> cadastro.getExcluidaEm() == null)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(placa));
        entity.setExcluidaEm(Instant.now());
        veiculos.save(entity);
    }

    private Veiculo paraDominio(VeiculoEntity entity) {
        ClienteEntity cliente = entity.getCliente();
        return new Veiculo(
                new Cliente(cliente.getNome(), CpfCnpj.de(cliente.getCpfCnpj())),
                Placa.de(entity.getPlaca()),
                entity.getMarca(),
                entity.getModelo(),
                entity.getAno()
        );
    }
}
