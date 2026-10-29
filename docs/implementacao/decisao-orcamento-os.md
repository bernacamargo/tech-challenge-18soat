# Aprovação e rejeição de orçamento — issue #40

## Implementado em domínio/aplicação

- Aprovação somente com orçamento em Aguardando aprovação, sem rejeição prévia.
- Validação e leitura do Clock antes de chamar `BaixaEstoquePort.baixarReservas(osId)`.
- Baixa bem-sucedida seguida de transição para Em execução, início registrado e gravação.
- Rejeição com data/motivo, preservando Aguardando aprovação, itens e orçamento (ADR 0012).
- Rejeição devolve as reservas ativas ao saldo livre e não inicia execução. Não se aprova orçamento rejeitado sem revisão.
- Testes sem Spring usando relógio fixo e ports fake, inclusive falhas e ordem de chamadas.

## Integração de aprovação com estoque

`AprovarOrcamento` e `RejeitarOrcamento` são registrados como beans e expostos sem JWT em
`POST /api/acompanhamento/ordens-de-servico/{id}/aprovacao` e `.../rejeicao` (ADR 0015).
Aprovar inicia a execução na mesma operação. Rejeitar cancela as reservas ativas.

`EstoqueOSAdapter` confere a correspondência exata de itens, vínculos e reservas, exige baixa
integral e participa da mesma transação da OS. Falha posterior ao consumo desfaz a baixa e a
transição. Reservas já baixadas só representam sucesso do port se comprovadamente corresponderem
aos itens; nova aprovação HTTP da OS em execução retorna 409 sem consumir novamente.
OS somente com serviços dispensa reservas. Ver ADR 0014 e a documentação da API.

Locks da OS e das peças protegem os comandos concorrentes de Atendimento. Os adaptadores
ficam na infraestrutura e não importam entidades externas no domínio/aplicação.
`IntegracaoEstoqueOSIntegrationTest` verifica baixa real, rollback, inconsistências e
concorrência; `ItensAprovacaoOSApiIntegrationTest` cobre o fluxo HTTP e autorização.

Revisão/reabertura, versionamento e liberação das reservas após rejeição estão fora desta
etapa e precisam de fluxo definido. A métrica de tempo médio será tratada por observabilidade
por outro responsável, conforme alinhamento do grupo; cálculo e endpoint próprios não fazem
parte desta entrega.

## Persistência da decisão

`OrdemDeServico.restaurar` aceita início e rejeição sem executar as ações de negócio.
`DecisaoOrcamentoPersistenceIntegrationTest` verifica reabertura, gravações sucessivas,
conclusão/entrega, motivo longo, precisão de microssegundos, constraints e rollback da OS.
A baixa usada nessa suíte é fake; as novas suítes de integração complementam a prova com estoque real.

`DecisaoOrcamentoMigrationIntegrationTest` migra um schema temporário da V5 para V5.1 com
OS e itens existentes, preservando dados e mantendo novos campos nulos. Não atribui início
fictício a OS legadas; isso precisa ser considerado nas métricas de observabilidade.
A consulta pública permanece limitada ao andamento. O detalhe/listagem administrativos expõem `iniciadaEm` e `rejeicaoOrcamento` por DTOs próprios, com campos nulos quando ausentes.

## Consulta administrativa da decisão

`RejeicaoOrcamentoResponse` representa momento e motivo sem expor o value object diretamente.
`OrdemDeServicoResponse` inclui esse objeto e o início, documentados no Swagger. A inclusão é
compartilhada por listagem, detalhe e respostas dos comandos administrativos existentes.
Testes de integração verificam os três perfis administrativos e impedem vazamento dos novos
campos na consulta pública, cuja resposta e esquema continuam com apenas dois campos.
