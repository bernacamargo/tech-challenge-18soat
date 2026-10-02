# ADR-0007: Notificações por e-mail via servidor SMTP local em Docker

- **Status**: Aceito
- **Data**: 2026-10-02
- **Participantes**: Grupo 117 (resposta do professor no fórum de dúvidas; proposta de SMTP local de Vitor Melo na reunião)

## Contexto

O fluxo de Ordem de Serviço exige **enviar o orçamento ao cliente para aprovação** (e, por
extensão, notificar mudanças de status). O mecanismo ficou pendente da resposta do professor no
fórum — a resposta confirmou que não há exigência de serviço externo (AWS/SES) nem de provedor de
e-mail real, abrindo caminho para a hipótese levantada na reunião: um **servidor SMTP local em
Docker**.

## Decisão

Usar o **MailPit** (`axllent/mailpit`) como servidor SMTP local no docker-compose (perfis
`dev`/`full`):

- **SMTP na porta 1025** e **interface web na 8025** para visualizar todos os e-mails enviados.
- A aplicação envia e-mails via **`spring-boot-starter-mail`** (`JavaMailSender`) apontando para o MailPit.
- Na Clean Architecture (ADR-0001): **port `NotificacaoPort` na camada application** do contexto
  `atendimento` (ex.: `EnviarOrcamentoUseCase` → `notificacaoPort.enviarOrcamento(...)`), com o
  **adapter SMTP na infrastructure**. Trocar MailPit por um provedor real no futuro é trocar o
  adapter + configuração — os casos de uso não mudam.

## Consequências

- Positivas: zero custo e zero contas externas; os e-mails ficam visíveis na UI do MailPit —
  evidência direta do fluxo de aprovação para o vídeo da entrega; protocolo SMTP real
  (código de produção); desacoplamento garantido pelo port.
- Negativas: os e-mails **não chegam a clientes reais** (são capturados localmente) — para um
  ambiente produtivo seria necessário um relay autenticado (fora do escopo da fase); mais um
  container no ambiente (~30 MB de RAM).
- Alternativas descartadas: **MailHog** (projeto descontinuado — MailPit é o sucessor),
  **GreenMail/Apache James** (mais pesados para o mesmo efeito em dev), **provedor externo**
  (SES/SendGrid — exige conta, segredos e risco de envio real na demonstração).
