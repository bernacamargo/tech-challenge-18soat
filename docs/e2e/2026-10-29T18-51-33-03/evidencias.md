# Teste ponta a ponta — 29/10/2026 18:51:33, America/Sao_Paulo

Validação do sistema da oficina no commit `1a51b79793d39620f435ad55844ae7be81d2be2f`. Esta execução fecha o que a das [16:27](../2026-10-29T16-27-53-03/evidencias.md) ainda não percorria: catálogo de demonstração, cadastro de cliente (CPF e CNPJ) e veículo, listagens, entrada de estoque, papéis do ciclo da ordem de serviço e a consulta pública com identificador inválido.

O ciclo completo da OS (troca de óleo até a entrega, estoque insuficiente, rejeição, vínculo na exclusão, limites, e-mail do orçamento e o 401 sem token) continua registrado na execução das 16:27. Juntas, as duas pastas cobrem atendimento, catálogo e estoque, identidade e notificação.

Resultado desta execução: **os cenários abaixo passaram.**

## Ambiente

A API, o Postgres, o MailPit e o Keycloak da execução anterior continuavam no ar. Issuer `http://localhost:8081/realms/oficina`. Tokens pelo password grant, client `oficina-api`, senha `demo123`, usuários `atendente`, `mecanico` e `administrador`.

O Swagger em `http://localhost:8080/swagger-ui/index.html` lista as operações do sistema. Expandir um POST nesse renderer travou o Chrome na execução anterior, então as chamadas abaixo foram feitas na API em `http://localhost:8080` com o JWT do Keycloak. O print mostra papel, rota, status e corpo.

![Lista de operações no Swagger](imagens/00-swagger.png)

## 1. Catálogo de demonstração

Mecânico em `GET /api/servicos`. Esperado 200 com os quatro serviços da carga (`...0101` a `...0104`). Obtido 200, com esses ids na lista.

![Serviços da demonstração](imagens/01-servicos-demo.png)

Atendente em `GET /api/pecas`. Esperado 200 com as cinco peças (`...0201` a `...0205`). Obtido 200.

![Peças da demonstração](imagens/02-pecas-demo.png)

## 2. Clientes e veículos

Atendente cadastrou a pessoa jurídica `19331077900165` (`Oficina Vizinha Ltda`). Esperado 201. Obtido 201.

![Cliente com CNPJ](imagens/03-cliente-cnpj.png)

Atendente cadastrou `Ana Ribeiro`, CPF `79331077963`, e-mail `ana.ribeiro@exemplo.com`. Esperado 201. Obtido 201.

![Cliente com CPF](imagens/04-cliente.png)

`GET /api/clientes` devolveu os dois documentos. Administrador alterou o nome para `Ana Ribeiro Costa` (200).

![Lista de clientes](imagens/05-clientes.png)

![Nome alterado](imagens/06-cliente-alterado.png)

Atendente cadastrou o veículo `ZXW7Q96` (Honda Civic, 2019) e alterou o modelo para `Civic EX`. A consulta pela placa devolveu o cliente e o modelo novo.

![Veículo cadastrado](imagens/07-veiculo.png)

![Veículo alterado](imagens/08-veiculo-alterado.png)

![Detalhe do veículo](imagens/09-veiculo-detalhe.png)

Mecânico em `GET /api/clientes`. Esperado 403. Obtido 403.

![Mecânico sem acesso a clientes](imagens/10-mecanico-clientes-403.png)

Repetir o `POST /api/clientes` do mesmo CPF. Esperado 409. Obtido 409, `Cliente ja cadastrado: 79331077963`.

![Cliente duplicado](imagens/18-cliente-duplicado-409.png)

## 3. Abertura da OS com cadastro existente

Mecânico em `POST /api/ordens-de-servico`. Esperado 403. Obtido 403.

![Mecânico não abre OS](imagens/11-mecanico-abre-os-403.png)

Atendente abriu a OS com o CPF e a placa já cadastrados, enviando outro nome no corpo. Esperado 201, `RECEBIDA`, cliente `79331077963` e veículo `ZXW7Q96`. Obtido a OS `a11f1ded-e90a-4f74-aba8-5ec2de1d857c`. O `GET` do cliente continuou com o nome `Ana Ribeiro Costa`: a abertura não sobrescreve o cadastro.

![OS do cliente existente](imagens/12-os-cliente-existente.png)

![Nome preservado](imagens/13-cliente-preservado.png)

Mecânico em `GET /api/ordens-de-servico`. A lista contém essa OS.

![Lista de ordens de serviço](imagens/14-lista-os.png)

## 4. Papéis e transição da execução

A OS ainda está `RECEBIDA`.

Atendente em `POST .../conclusao`. Esperado 403. Obtido 403.

![Atendente não conclui](imagens/15-atendente-conclusao-403.png)

Mecânico em `POST .../entrega`. Esperado 403. Obtido 403.

![Mecânico não entrega](imagens/16-mecanico-entrega-403.png)

Mecânico em `POST .../conclusao` com a OS recebida. Esperado 409. Obtido 409, `Transição de RECEBIDA para FINALIZADA não permitida`.

![Conclusão fora de estado](imagens/17-conclusao-cedo-409.png)

O diagnóstico negado ao atendente e a entrega feita por ele, no fluxo feliz, estão na execução das 16:27.

## 5. Acompanhamento público

`GET /api/acompanhamento/ordens-de-servico/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa`, sem token. Esperado 404. Obtido 404, `Ordem de serviço não encontrada`.

![OS inexistente](imagens/19-publico-404.png)

O mesmo recurso com `nao-e-uuid`. Esperado 400. Obtido 400.

![Identificador inválido](imagens/20-publico-400.png)

## 6. Manutenção do catálogo e entrada de estoque

Administrador cadastrou o serviço `Revisao de suspensao` a R$ 210,00 (201) e alterou o preço para R$ 230,00 (200).

![Serviço cadastrado](imagens/21-servico.png)

![Preço alterado](imagens/22-servico-alterado.png)

Administrador cadastrou a peça `Lampada do farol` com saldo inicial 4 (201) e lançou uma entrada de 6. Esperado saldo físico 10, reservado 0, livre 10. Obtido isso no corpo da entrada e no `GET` seguinte, feito pelo atendente.

![Peça cadastrada](imagens/23-peca.png)

![Entrada de 6](imagens/24-entrada.png)

![Saldo 10](imagens/25-peca-saldo.png)

Mecânico na mesma entrada, quantidade 1. Esperado 403. Obtido 403. A leitura do saldo pelo atendente continua 200 (print anterior).

![Mecânico não movimenta estoque](imagens/26-mecanico-entrada-403.png)

A leitura do catálogo por atendente e mecânico, a escrita negada a esses papéis, a exclusão com vínculo e os limites de nome, preço e overflow continuam na execução das 16:27.
