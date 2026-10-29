/**
 * Camada de dominio de Identidade e Acesso, sem dependencia de framework: Perfil (perfil de
 * acesso da oficina: ATENDENTE, MECANICO, ADMINISTRADOR; no Keycloak, realm role), Credencial
 * (access token Bearer, valor opaco), DadosDaCredencial (dados crus verificados pelo
 * provedor), UsuarioAdministrativo (identidade autenticada por requisicao, nao persistida),
 * as excecoes CredencialInvalidaException e UsuarioSemPerfilDaOficinaException e o contrato
 * ValidadorDeCredencial, port definido no dominio pela issue #4 e implementado na
 * infraestrutura (issue #6).
 */
package br.com.grupo117.oficina.identidadeacesso.domain;
