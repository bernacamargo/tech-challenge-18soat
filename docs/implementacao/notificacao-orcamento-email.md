# Envio do orçamento por e-mail

A geração do orçamento agenda automaticamente uma mensagem ao entrar em Aguardando aprovação.
Funciona nas rotas da PR #74:

- `POST /api/ordens-de-servico/{id}/diagnostico/conclusao`;
- `POST /api/ordens-de-servico/{id}/submissao`.

Não é necessário chamar uma rota de envio. Em condições normais, a mensagem aparece em até
5 segundos após o commit. O scheduler usa uma fila PostgreSQL persistente. A decisão está no
[ADR 0017](../adr/0017-notificacao-orcamento-email.md).

## Cadastrar ou atualizar o contato

Com JWT de ATENDENTE/ADMINISTRADOR, cadastrar em `POST /api/clientes`:

```json
{
  "nome": "Maria Souza",
  "cpfCnpj": "52998224725",
  "email": "maria@exemplo.com"
}
```

Para atualizar: `PUT /api/clientes/52998224725`, com `nome` e `email`. Omitir o e-mail ou
informar null preserva o contato anterior; string vazia/inválida retorna 400. GET/listagem
administrativos incluem o contato, mas a consulta pública de OS continua sem dados pessoais.
Também é possível informar `email` no JSON de abertura da OS: cadastra o contato de um cliente
novo ou preenche um cadastro antigo sem contato. Não substitui contato existente; use PUT
para alterá-lo explicitamente.

Clientes antigos continuam válidos. Se o orçamento for gerado sem contato, a fila fica com
`AGUARDANDO_EMAIL`. Após atualizar o cadastro, a tentativa seguinte (até 1 minuto) enviará a
mensagem. O snapshot do orçamento preserva valores e itens do momento da geração.

## Ambiente local

```bash
# Apenas PostgreSQL e MailPit; rodar a aplicação via Maven:
docker compose --profile dev up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# Alternativa completa:
docker compose --profile full up -d --build
```

Abra http://localhost:8025 para ler os e-mails capturados. Eles não são entregues a caixas
postais externas. O corpo inclui identificação da OS, veículo, serviços/peças, quantidades,
preços e total. O campo remetente é `no-reply@oficina.local` por padrão.

Configuração por variáveis de ambiente:

| Variável | Padrão | Uso |
|---|---|---|
| `SMTP_HOST` | `localhost` (`mailpit` no Compose) | Servidor SMTP |
| `SMTP_PORT` | `1025` | Porta SMTP |
| `SMTP_FROM` | `no-reply@oficina.local` | Remetente |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | vazios | Credenciais do provedor, quando necessário |
| `SMTP_AUTH` / `SMTP_STARTTLS` | `false` | Autenticação/STARTTLS do provedor |
| `NOTIFICACOES_HABILITADAS` | `true` | Habilita o dispatcher; desabilitar preserva a fila |

Credenciais devem ser definidas no ambiente, nunca no repositório. Não publicar as portas do
MailPit em rede pública. Ao integrar a PR #79, manter uma única definição de MailPit e permitir
SMTP :1025 em loopback para desenvolvimento via Maven.

## Falhas e acompanhamento

Se o SMTP falhar, a resposta da geração do orçamento permanece válida. A fila recebe
`FALHA_SMTP` e tenta novamente após um minuto. O envio não aprova a OS nem movimenta estoque.
Após aprovação ou rejeição, uma solicitação ainda pendente é cancelada pelo dispatcher na
próxima tentativa, preservando snapshot, tentativas e `cancelada_em`. A OS é bloqueada durante
a decisão de envio e o SMTP: se a decisão já confirmou, não envia; se o envio começou antes,
a decisão aguarda seu término. Mensagens enviadas não são reenviadas normalmente; uma queda entre aceite SMTP e gravação do
sucesso pode duplicar a entrega (garantia pelo menos uma vez).

Consulta administrativa local, sem conteúdo ou destinatário:

```sql
SELECT ordem_de_servico_id, criada_em, enviada_em, cancelada_em, tentativas,
       proxima_tentativa_em, ultimo_erro
FROM notificacao_orcamento
ORDER BY criada_em DESC;
```

## Testes

`./mvnw verify` executa regressão e cobertura. As novas suítes verificam e-mail válido/inválido,
compatibilidade com clientes antigos, preservação do contato, abertura de OS com e-mail,
confidencialidade, os dois comandos HTTP de geração, conteúdo do orçamento, não duplicação
normal, rollback, falha da fila, retry SMTP e ausência de contato. `OrcamentoSmtpIntegrationTest`
usa PostgreSQL e MailPit reais em containers isolados, confirmando envio automático pelo
scheduler e leitura do conteúdo recebido. Os containers de teste são descartados ao final.

Validação local em 2026-10-26: `./mvnw -B verify` terminou com **520 testes, zero falhas/erros**
e gate JaCoCo aprovado. Compose validado com `docker compose --profile full config --quiet`.
O envio SMTP foi verificado no MailPit isolado; o banco local de demonstração não foi alterado.
