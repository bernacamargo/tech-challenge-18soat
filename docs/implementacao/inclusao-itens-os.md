# Inclusão de itens da OS — preparação da issue #34

## Domínio

`ItemDaOS` é um item identificado por UUID, imutável, contendo o identificador do catálogo,
tipo (SERVICO/PECA), descrição, quantidade positiva e preço unitário positivo. Seu subtotal é
quantidade × preço unitário. `Dinheiro` usa BigDecimal, sem arredondamento implícito e com
precisão de centavos. O item preserva descrição/preço vigentes na inclusão.

O agregado só admite inclusão em Recebida ou Em diagnóstico. Aguardando aprovação e estados
posteriores bloqueiam alterações nesta implementação. Identificadores de itens não se repetem;
a coleção exposta é imutável. Dois itens podem referenciar o mesmo produto do catálogo.

## Aplicação e contratos propostos

- `application/usecase/IncluirServicoNaOS`: busca OS e serviço, valida e adiciona o item.
- `application/usecase/IncluirPecaNaOS`: busca OS e peça, valida todos os dados antes de reservar,
  exige reserva bem-sucedida, adiciona o item e salva a OS.
- `application/port/CatalogoOSPort`: consulta serviço/peça e retorna `ItemCatalogo`, uma projeção
  com identificador UUID, descrição e preço BigDecimal, sem entidades do contexto externo.
- `application/port/ReservaEstoquePort`: reserva atomicamente por OS/item/peça/quantidade. Sem
  disponibilidade retorna false sem efeitos. itemId deve permitir idempotência no adaptador;
  repetir a mesma reserva não deve descontar duas vezes. Reserva não é baixa.

Os contratos são propostas para integração com Bernardo/Vitor, não contratos já confirmados
do catálogo. Não existe verificação de disponibilidade separada da reserva, para evitar a
janela de concorrência entre consulta de saldo e reserva.

## Pendências antes da integração real

A issue #34 não está concluída ponta a ponta. Os casos de uso são testados com fakes e **não
estão registrados como beans nem expostos por endpoints**. Dependem dos adaptadores de catálogo
e estoque (#24/#25/#30) e do alinhamento de identificadores.

O adaptador JPA persiste e reconstitui itens e orçamento via migration V5, com testes em
PostgreSQL e exposição no detalhe administrativo (ADR 0011).

Reserva e gravação da OS devem participar da mesma transação Postgres na integração, garantindo
rollback da reserva se a gravação falhar. Os fakes não demonstram rollback, concorrência ou
idempotência do futuro adaptador. O contrato não torna uma nova requisição HTTP idempotente.

## Verificação

Testes de domínio validam preços, quantidades, subtotal, coleção imutável e todos os estados.
Testes de aplicação verificam inclusão dos dois tipos, preço preservado, OS/item ausentes,
estado inválido, dados divergentes do catálogo, estoque insuficiente e falha de reserva.
