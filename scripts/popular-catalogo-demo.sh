#!/usr/bin/env bash
# Carga opcional, executada explicitamente após a inicialização da aplicação/Flyway.
set -euo pipefail

if [[ $# -ne 0 ]]; then
    echo "Uso: $0" >&2
    exit 2
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
if ! command -v docker >/dev/null 2>&1; then
    echo 'Docker não encontrado. Instale Docker e Docker Compose antes de continuar.' >&2
    exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
    echo 'Docker Compose não encontrado (comando: docker compose).' >&2
    exit 1
fi

# O serviço deve estar pronto; não cria nem reinicia containers e não apaga dados.
if ! docker compose --project-directory "$repo_dir" exec -T postgres sh -c \
    'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1; then
    echo 'PostgreSQL indisponível. Na raiz do projeto, execute: docker compose --profile dev up -d' >&2
    echo 'Depois inicie a aplicação e aguarde as migrations Flyway.' >&2
    exit 1
fi

echo 'Carregando catálogo de demonstração; registros existentes serão preservados...'
docker compose --project-directory "$repo_dir" exec -T postgres sh -c \
    'exec psql --no-psqlrc --set=ON_ERROR_STOP=1 --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' \
    < "$repo_dir/scripts/demo/catalogo.sql"
echo 'Carga concluída. Reexecutar não duplica os IDs nem repõe o estoque.'
