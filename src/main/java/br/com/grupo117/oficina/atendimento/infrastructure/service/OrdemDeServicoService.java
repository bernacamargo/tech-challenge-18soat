package br.com.grupo117.oficina.atendimento.infrastructure.service;

import br.com.grupo117.oficina.atendimento.application.usecase.CriarOrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.DetalharOrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.ListarOrdensDeServico;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.usecase.IniciarDiagnostico;
import br.com.grupo117.oficina.atendimento.application.usecase.ConcluirServicos;
import br.com.grupo117.oficina.atendimento.application.usecase.RegistrarEntrega;
import br.com.grupo117.oficina.atendimento.application.usecase.GerarOrcamento;
import br.com.grupo117.oficina.atendimento.application.usecase.SubmeterOrdemDeServico;
import br.com.grupo117.oficina.atendimento.infrastructure.persistence.OperacaoOSLock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fronteira transacional; mantém os casos de uso independentes do Spring. */
@Service
@Transactional
public class OrdemDeServicoService {
    private final CriarOrdemDeServico criar;
    private final ListarOrdensDeServico listar;
    private final DetalharOrdemDeServico detalhar;

    private final IniciarDiagnostico iniciarDiagnostico;
    private final ConcluirServicos concluirServicos;
    private final RegistrarEntrega registrarEntrega;
    private final GerarOrcamento gerarOrcamento;
    private final SubmeterOrdemDeServico submeterOrdem;
    private final OperacaoOSLock locks;

    public OrdemDeServicoService(CriarOrdemDeServico criar, ListarOrdensDeServico listar,
                                  DetalharOrdemDeServico detalhar, IniciarDiagnostico iniciarDiagnostico,
                                  ConcluirServicos concluirServicos, RegistrarEntrega registrarEntrega,
                                  GerarOrcamento gerarOrcamento, SubmeterOrdemDeServico submeterOrdem,
                                  OperacaoOSLock locks) {
        this.criar = criar;
        this.listar = listar;
        this.detalhar = detalhar;
        this.iniciarDiagnostico = iniciarDiagnostico;
        this.concluirServicos = concluirServicos;
        this.registrarEntrega = registrarEntrega;
        this.gerarOrcamento = gerarOrcamento;
        this.submeterOrdem = submeterOrdem;
        this.locks = locks;
    }

    public OrdemDeServico criar(String documento, String nome, String placa, String marca, String modelo, int ano) {
        return criar.criar(documento, nome, placa, marca, modelo, ano);
    }

    public OrdemDeServico criar(String documento, String nome, String placa, String marca, String modelo, int ano,
                               UUID servicoId, UUID pecaId, Integer quantidade) {
        return criar.criar(documento, nome, placa, marca, modelo, ano, servicoId, pecaId, quantidade);
    }

    @Transactional(readOnly = true)
    public List<OrdemDeServico> listar() { return listar.listar(); }

    @Transactional(readOnly = true)
    public OrdemDeServico detalhar(UUID id) { return detalhar.detalhar(id); }

    public OrdemDeServico iniciarDiagnostico(UUID id) { locks.bloquearOrdem(id); return iniciarDiagnostico.iniciar(id); }

    public OrdemDeServico concluirServicos(UUID id) { locks.bloquearOrdem(id); return concluirServicos.concluir(id); }

    public OrdemDeServico registrarEntrega(UUID id) { locks.bloquearOrdem(id); return registrarEntrega.registrar(id); }

    public OrdemDeServico concluirDiagnostico(UUID id) { locks.bloquearOrdem(id); return gerarOrcamento.gerar(id); }

    public OrdemDeServico submeter(UUID id) { locks.bloquearOrdem(id); return submeterOrdem.submeter(id); }

    public OrdemDeServico gerarOrcamento(UUID id) { return concluirDiagnostico(id); }
}
