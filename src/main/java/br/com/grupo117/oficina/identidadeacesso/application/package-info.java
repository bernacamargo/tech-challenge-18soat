/**
 * Camada de aplicacao de Identidade e Acesso: caso de uso AutenticarUsuarioAdministrativo,
 * que recebe a Credencial (access token Bearer emitido pelo Keycloak) e usa o contrato
 * ValidadorDeCredencial do dominio como port de saida para devolver o UsuarioAdministrativo
 * autenticado ou a falha. Contas, senhas e tokens pertencem ao Keycloak (ADR 0006): esta
 * camada so consome a credencial e nao cadastra usuarios.
 */
package br.com.grupo117.oficina.identidadeacesso.application;
