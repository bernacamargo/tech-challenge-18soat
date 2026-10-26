# ADR 0017 — Contato do cliente e envio durável do orçamento por SMTP

- Status: Proposto
- Data: 2026-10-26
- Complementa ADR 0007 e os comandos do Event Storming (PR #74)

## Contexto

A oficina deve enviar o orçamento ao cliente assim que ele estiver pronto. O cadastro atual
não possui e-mail. Conclusão do diagnóstico e submissão da OS geram orçamento; ambas precisam
agendar o mesmo envio. SMTP não participa da transação PostgreSQL e pode ficar indisponível.

## Decisão

Adicionar o VO `Email` ao cliente, sem dependência de framework. Expor o contato no CRUD
administrativo e aceitar o campo na abertura da OS. A migration V9 é aditiva, sem atribuir
endereços fictícios a clientes antigos. Por compatibilidade, o contato é opcional; campos
omitidos/nulos preservam o endereço na atualização. Um endereço informado em branco ou inválido
é rejeitado. Identificar um cliente na OS preenche apenas contato ausente; para trocar um
contato existente, usar o CRUD protegido de clientes. O acompanhamento público não expõe e-mail.

Os casos de uso de geração e submissão chamam `NotificacaoPort` após salvar a OS. O adaptador
insere um snapshot de itens, quantidades, preços e total na tabela `notificacao_orcamento`,
na mesma transação. Falha nessa inserção desfaz a geração. Não enviar SMTP no controller nem
na transação que cria o orçamento.

Um dispatcher em infraestrutura executa a cada 5 segundos, em transação independente, lendo
somente dados confirmados. Busca até 10 mensagens com `FOR UPDATE SKIP LOCKED`, prevenindo o
processamento simultâneo da mesma linha. Envia com `JavaMailSender` e marca o horário do envio.
Timeouts SMTP são de 3 segundos. Falhas preservam a mensagem e reagendam para um minuto depois.
O orçamento continua válido e aguardando decisão. Credenciais e mensagens de erro SMTP não
são armazenadas nos logs nem na fila; apenas códigos genéricos de falha.

Uma mensagem por OS é garantida por constraint única. A mensagem guarda o destinatário
conhecido na geração. Se ausente, aguarda o cadastro e resolve o contato no envio. O corpo
é texto simples com UUID da OS, veículo, itens e valor; não contém CPF nem credenciais e
não depende de frontend ou de uma chamada GET que acione aprovação.

MailPit é o SMTP de demonstração, com UI em loopback :8025. A porta SMTP :1025 é publicada
em loopback para a aplicação via Maven; app em container usa `mailpit:1025`. O mesmo serviço
pode ser compartilhado com o Keycloak da PR #79. Não criar outro MailPit ao integrar as branches.

## Consequências

A aplicação não perde a intenção de notificar ao reiniciar nem envia orçamento revertido.
Cadastros sem e-mail continuam funcionando e acumulam envio aguardando contato. O reenvio
é automático; não adicionamos endpoint público para manipular a fila.

A entrega é **pelo menos uma vez**: se o SMTP aceitar e o processo cair antes de confirmar
`enviada_em`, a próxima tentativa pode repetir o e-mail. SMTP não oferece transação distribuída
ou garantia de exatamente uma entrega. Repetir o comando da OS ou executar novamente o worker
após sucesso confirmado não cria outro envio.

A fila usa PostgreSQL; não introduz broker. Segurar o lock durante SMTP simplifica exclusão
mútua no MVP, com limites e timeouts; se o volume crescer, reavaliar claim/lease e lotes.
Não há fluxo de revisão/versionamento de orçamento nesta fase; ao adicioná-lo, revisar a
unicidade por OS e a política de notificação por versão.

## Evidências

Verificação local em 2026-10-26: 520 testes passaram com gate de cobertura aprovado.
Inclui domínio/aplicação para contato e agendamento, integração PostgreSQL para gravação
atômica, migração de dados antigos e retries, e MailPit real em Testcontainers para comprovar
transporte SMTP automático e conteúdo.
