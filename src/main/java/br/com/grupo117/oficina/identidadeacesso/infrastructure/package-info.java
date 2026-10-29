/**
 * Camada de infraestrutura de Identidade e Acesso: adapter KeycloakValidadorDeCredencial
 * (implementa o port ValidadorDeCredencial com o JwtDecoder do Spring Boot), o
 * AuthenticationManager do resource server que delega ao caso de uso
 * AutenticarUsuarioAdministrativo e a configuracao que liga as pecas (ADR 0006).
 * Nao ha tabela de usuario nem senha: a conta vive no Keycloak.
 */
package br.com.grupo117.oficina.identidadeacesso.infrastructure;
