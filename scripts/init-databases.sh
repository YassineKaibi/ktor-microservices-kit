#!/bin/bash
set -e

# Create application databases if they don't already exist.
# This script runs inside the PostgreSQL container via /docker-entrypoint-initdb.d/.

AUTH_DB="${AUTH_DB:-auth_db}"
USER_DB="${USER_DB:-user_db}"
CRUD_DB="${CRUD_DB:-crud_db}"

for DB in "$AUTH_DB" "$USER_DB" "$CRUD_DB"; do
  echo "Creating database: $DB"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
    SELECT 'CREATE DATABASE $DB'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB')\gexec
SQL
done
