# Geração de orçamento — issue #38

`application/usecase/GerarOrcamento` busca a OS por UUID, delega o cálculo ao agregado e salva
após sucesso. `Orcamento` é um value object imutável com cópia dos itens e total derivado da soma
de quantidade × preço unitário de cada item (serviços e peças). Não consulta preços atuais do
catálogo, não recebe total externo e não usa ponto flutuante.

A geração ocorre somente em Em diagnóstico e leva a Aguardando aprovação. Como regra desta
implementação, exige ao menos um item: OS vazia permanece em diagnóstico. Inclusão de itens
fica bloqueada após gerar orçamento; uma nova geração em Aguardando aprovação é rejeitada.
Fluxos de ajuste após rejeição serão tratados na #40.

Testes com ports fake verificam total, quantidades, estado, imutabilidade, OS ausente, identificador
nulo e rejeição dos cinco estados inválidos, sem salvar a OS nesses casos.

## Integração REST e persistência

`POST /api/ordens-de-servico/{id}/diagnostico/conclusao` aciona o caso de uso por
`OrdemDeServicoService`, em transação, exigindo MECANICO ou ADMINISTRADOR. Sem corpo de
requisição; retorna a OS com orçamento e status atualizado. OS vazia retorna 400 e geração
fora de Em diagnóstico retorna 409. Repetição não sobrescreve o orçamento anterior.
`POST /{id}/submissao` aceita também a OS recebida.

O adaptador JPA preserva itens, preços e total, verificando consistência ao reconstituir o
agregado (ADR 0011). Testes de integração cobrem geração, consulta, segurança e rollback real.

## Pendências

Inclusão real de itens depende dos adaptadores de catálogo/estoque e endpoints da #34.
Envio de e-mail ao cliente, aprovação e baixa não fazem parte deste endpoint. A entrega completa
da #38 deve ser avaliada junto ao fluxo de envio previsto no enunciado e no ADR 0007.
