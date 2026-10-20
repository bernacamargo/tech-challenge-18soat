# Aprovação e rejeição de orçamento — issue #40

## Implementado em domínio/aplicação

- Aprovação somente com orçamento em Aguardando aprovação, sem rejeição prévia.
- Validação e leitura do Clock antes de chamar `BaixaEstoquePort.baixarReservas(osId)`.
- Baixa bem-sucedida seguida de transição para Em execução, início registrado e gravação.
- Rejeição com data/motivo, preservando Aguardando aprovação, itens e orçamento (ADR 0012).
- Rejeição não inicia execução nem aciona baixa. Não se aprova orçamento rejeitado sem revisão.
- Testes sem Spring usando relógio fixo e ports fake, inclusive falhas e ordem de chamadas.

## Integração pendente

Estes casos de uso NÃO são beans Spring nem endpoints. O adaptador JPA existente ainda não
preserva `iniciadaEm` e `rejeicaoOrcamento`: conectá-lo agora perderia esses dados. Antes do
wiring são necessários migration, reconstituição, DTOs administrativos e testes reais de
persistência/rollback. A autorização dos comandos do cliente também precisa ser definida.

A PR #51 propõe `EstoqueDaOrdemDeServico.baixarReservasDaOrdemDeServico(osId)`, retornando uma
contagem. Atendimento propõe um port próprio com sucesso integral ou exceção; o adaptador futuro
deve alinhar os contratos, conferir as reservas correspondentes aos itens e compartilhar a
transação PostgreSQL com a OS. Não basta ignorar um retorno zero quando há peças que deveriam
ter reservas. A PR #52 fornece infraestrutura ainda não integrada à main.

O contrato de reserva por item da #34 também precisa de alinhamento com o estoque. Os fakes
NÃO provam idempotência, concorrência ou rollback de estoque real; isso continua pendente.
Falha na baixa impede mutação/salvamento da OS; falha posterior ao consumo deverá desfazer
ambos pela transação real. Não há compensação artificial no caso de uso.

Revisão/reabertura, versionamento e liberação das reservas após rejeição estão fora desta
etapa e precisam de fluxo definido. O cálculo e a API do tempo médio (#39) ainda não existem.
