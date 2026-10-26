/**
 * Bounded Context de Identidade e Acesso, conformista com o Keycloak (relacao de contexto do
 * DDD; a escolha do Keycloak e da ADR 0006). A credencial do usuario administrativo e o
 * access token Bearer emitido pelo Keycloak; a oficina nao armazena senhas nem contas de
 * usuario, que sao administradas no Keycloak.
 * O dominio monta, a cada requisicao, a identidade autenticada (UsuarioAdministrativo) com
 * os perfis da oficina; a validacao do token pela API fica na infraestrutura (issue #6).
 */
package br.com.grupo117.oficina.identidadeacesso;
