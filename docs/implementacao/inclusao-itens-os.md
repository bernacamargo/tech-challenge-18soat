# Inclusão de itens da OS — issue #34

## Domínio

`ItemDaOS` é um item identificado por UUID, imutável, contendo o identificador do catálogo,
tipo (SERVICO/PECA), descrição, quantidade positiva e preço unitário positivo. Seu subtotal é
quantidade × preço unitário. `Dinheiro` usa BigDecimal, sem arredondamento implícito e com
precisão de centavos. O item preserva descrição/preço vigentes na inclusão.

O agregado só admite inclusão em Recebida ou Em diagnóstico. Aguardando aprovação e estados
posteriores bloqueiam alterações nesta implementação. Identificadores de itens não se repetem;
a coleção exposta é imutável. Dois itens podem referenciar o mesmo produto do catálogo.

## Aplicação e contratos

- `application/usecase/IncluirServicoNaOS`: busca OS e serviço, valida e adiciona o item.
- `application/usecase/IncluirPecaNaOS`: busca OS e peça, valida todos os dados antes de reservar,
  exige reserva bem-sucedida, adiciona o item e salva a OS.
- `application/port/CatalogoOSPort`: consulta serviço/peça e retorna `ItemCatalogo`, uma projeção
  com identificador UUID, descrição e preço BigDecimal, sem entidades do contexto externo.
- `application/port/ReservaEstoquePort`: reserva atomicamente por OS/item/peça/quantidade. Sem
  disponibilidade retorna false sem efeitos. itemId deve permitir idempotência no adaptador;
  repetir a mesma reserva não deve descontar duas vezes. Reserva não é baixa.

## Integração real com catálogo e estoque

`CatalogoOSAdapter` consulta os repositórios de Catálogo/Estoque e projeta nome/preço nos ports
de Atendimento. `EstoqueOSAdapter` adapta a reserva externa, que retorna um ID de reserva,
ao contrato por item usando `reserva_item_os` (migration V8). Ver ADR 0014.

`IntegracaoCatalogoEstoqueOSConfig` registra os casos de uso e `ItensOrcamentoOSService`
executa cada comando em uma transação PostgreSQL conjunta com o estoque. Locks da OS e da
peça evitam perda de itens e disputa de saldo. Falhas de inclusão/gravação revertem reserva,
vínculo e saldos. Repetição do port com o mesmo item é idempotente; parâmetros divergentes
são recusados. Uma nova requisição HTTP cria outro item e não é idempotente.

`ItensOrcamentoOSController` expõe POST `/api/ordens-de-servico/{id}/servicos` e `/pecas`.
Requisições e papéis estão em [API de OS](../api/ordens-de-servico.md). Os itens e orçamento
continuam persistidos pela V5 e expostos no detalhe administrativo (ADR 0011).

## Verificação

Testes de domínio validam preços, quantidades, subtotal, coleção imutável e todos os estados.
Testes de aplicação verificam inclusão dos dois tipos, preço preservado, OS/item ausentes,
estado inválido, dados divergentes do catálogo, estoque insuficiente e falha de reserva.

`IntegracaoEstoqueOSIntegrationTest` usa PostgreSQL real para verificar reserva/baixa,
rollback, vínculos, FK diferida e concorrência. `ItensAprovacaoOSApiIntegrationTest` cadastra
catálogo e OS pelas APIs, inclui itens, gera/aprova orçamento e conclui/entrega a OS.
