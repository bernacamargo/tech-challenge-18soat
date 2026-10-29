-- Roles e schemas do banco unico oficina (idempotente; roda a cada subida, antes do Keycloak e da app).
-- oficina_app: dona do schema public (tabelas da oficina e Flyway). keycloak: dona do schema keycloak.
-- Nenhuma senha neste arquivo: vem de APP_DB_PASSWORD e KEYCLOAK_DB_PASSWORD (ADR 0020).
\set ON_ERROR_STOP on
\getenv app_password APP_DB_PASSWORD
\getenv kc_password KEYCLOAK_DB_PASSWORD
SET client_min_messages = warning;

SELECT format('CREATE ROLE oficina_app LOGIN PASSWORD %L', :'app_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'oficina_app') \gexec
SELECT format('ALTER ROLE oficina_app WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', :'app_password') \gexec
SELECT format('CREATE ROLE keycloak LOGIN PASSWORD %L', :'kc_password')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'keycloak') \gexec
SELECT format('ALTER ROLE keycloak WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', :'kc_password') \gexec

ALTER SCHEMA public OWNER TO oficina_app;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
-- tabelas e sequencias criadas antes pelo superusuario (pgdata existente)
SELECT format('ALTER TABLE public.%I OWNER TO oficina_app', tablename)
  FROM pg_tables WHERE schemaname = 'public' AND tableowner <> 'oficina_app' \gexec
SELECT format('ALTER SEQUENCE public.%I OWNER TO oficina_app', sequencename)
  FROM pg_sequences WHERE schemaname = 'public' AND sequenceowner <> 'oficina_app' \gexec

CREATE SCHEMA IF NOT EXISTS keycloak AUTHORIZATION keycloak;
ALTER ROLE keycloak IN DATABASE oficina SET search_path = keycloak;
