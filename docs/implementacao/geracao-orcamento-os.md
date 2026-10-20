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

## Integração pendente

O caso de uso ainda não está ligado ao Spring/REST. O adaptador JPA agora preserva itens, preços
e total do orçamento, com verificação de consistência ao reconstituir o agregado (ADR 0011).
O detalhe administrativo expõe itens e total do orçamento nesta camada de API (#62).
Envio ao cliente, aprovação e baixa não fazem parte da geração de orçamento.
