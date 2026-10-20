# ADR-0008: Persistência da OS e fronteira transacional na infraestrutura

- **Status**: Proposto
- **Data**: 2026-10-12
- **Participantes**: Proposta para revisão do Grupo 117 (issue #36)

## Contexto

A abertura da OS pode cadastrar cliente e veículo antes de salvar a ordem. Transações isoladas
por adaptador permitem cadastros parciais se uma etapa falhar. O domínio e os casos de uso
existentes permanecem independentes de Spring. A #36 exige persistência e consultas REST.

## Decisão

Criar `ordem_de_servico` com UUID, documento do cliente, placa do veículo, status textual e
`finalizada_em` em UTC (`TIMESTAMP WITH TIME ZONE`). Chaves estrangeiras preservam as referências
históricas e impedem excluir cadastros referenciados por OS. Sem exclusão em cascata.

Usar entidade JPA separada do agregado, com fábrica de reconstituição que valida a coerência
entre status e finalização. Um serviço na infraestrutura envolve a abertura inteira em
`@Transactional`; consultas são read-only. Casos de uso continuam sem Spring.

Organizar os novos arquivos em `infrastructure/api`, `persistence`, `config` e `service`.
Proteger criação para ATENDENTE/ADMINISTRADOR e consultas também para MECANICO por autorização
no método, reutilizando o decoder JWT existente sem alterar os arquivos dos cadastros.

## Consequências

- Cliente, veículo e OS são gravados atomicamente; falhas provocam rollback.
- Histórico de OS impede remover fisicamente o cliente/veículo referenciado. Evolução do CRUD
  para tratar explicitamente esse conflito fica fora desta base.
- Itens, orçamento e início de execução ainda não fazem parte desta migration; serão adicionados
  pelas próximas issues. A #36 não está completa até o detalhe incluir itens e orçamento.
- A infraestrutura local Keycloak/MailPit continua pendente da #6. Testes usam JWT assinado
  conforme o suporte já existente no repositório.
- Uma fachada transacional acrescenta uma classe, mas evita dependência de Spring na aplicação.
  Alternativa: anotar casos de uso; descartada para manter o padrão dos casos de uso existentes.
