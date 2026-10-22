# ADR 0011 — Persistência de itens e orçamento da OS

- Status: Proposto
- Contexto: issues #34, #36 e #38

## Contexto

Os itens e o orçamento já existem no domínio, mas o adaptador de persistência guardava somente
os dados básicos da OS. Salvar e reabrir o agregado perdia o estado necessário ao detalhamento.

## Decisão

Persistir itens como coleção de componentes JPA em `item_da_os`, pertencente ao agregado OS.
Cada linha guarda UUID do item, referência UUID ao catálogo, tipo, descrição, quantidade,
preço unitário e posição. O domínio permanece sem dependência de JPA. As referências ao catálogo
não têm FK enquanto os contratos dos outros contextos estão pendentes.

Guardar o total do orçamento em `ordem_de_servico.orcamento_total`; nulo representa orçamento
ainda não gerado. Os itens existentes são o snapshot do orçamento: o domínio bloqueia sua
alteração após a geração. Na leitura, reconstruir o orçamento e conferir seu total contra o
valor persistido, rejeitando divergência. Não recalcular preços a partir do catálogo.

A migration V5 inclui constraints de valores positivos, tipos e posição única por OS, além
de impedir orçamento em Recebida/Em diagnóstico. Remover uma OS remove seus itens por cascade.
Gravação de itens e cabeçalho usa a transação já existente no adaptador.

O detalhe e a listagem administrativos incluem itens e total. A consulta pública continua
expondo somente status e finalização.

## Consequências

Gravações sucessivas e leituras preservam identidade, ordem, descrição e preço. Não há histórico
de versões do orçamento; ajustes após rejeição (#40) exigirão avaliar a representação antes
de permitir modificar itens orçados. Esta decisão não implementa adaptadores de catálogo,
reserva de estoque nem endpoints de inclusão/geração. A futura reserva e gravação da OS devem
compartilhar a transação PostgreSQL.

## Evidências

Testes de integração com PostgreSQL verificam round-trip, gravações sucessivas sem duplicar
itens, total inconsistente, rollback após falha na coleção e respostas administrativa/pública.
