# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A production-oriented microservices starter kit built with Ktor (Kotlin) and Nginx. Uses the repository pattern for database-agnostic data access - ships with PostgreSQL and Redis defaults, but any data store can be swapped per-service.

## Common Commands

```bash
# Start all services
docker compose up -d

# View service status
docker compose ps

# View logs for a specific service
docker compose logs -f nginx
docker compose logs -f auth-service
docker compose logs -f user-service
docker compose logs -f crud-service

# Stop all services
docker compose down

# Rebuild a specific service
docker compose build auth-service

# Run tests for a service (from service directory)
./gradlew test

# Build a service
./gradlew build

# Health check
curl http://localhost:8080/auth/health
```

## Architecture

### Service Ports
- **Nginx Gateway**: 8080 (single entry point)
- **Auth Service**: 8081
- **User Service**: 8082
- **CRUD Service**: 8083

### Request Flow
1. All traffic enters via Nginx gateway on port 8080
2. Protected routes trigger `auth_request` to Auth Service `/auth/validate`
3. Auth Service validates JWT and returns user ID in `X-User-Id` header
4. Gateway forwards request with user context to downstream service

### Database Pattern
Each service owns its database (database-per-service pattern):
- `auth_db`: Credentials and token metadata
- `user_db`: Profile data
- `crud_db`: Generic items

Redis is shared for rate limiting (Nginx) and token blacklist (Auth Service).

### Repository Pattern
Services depend on repository interfaces, not implementations:
```
ItemRepository (interface)
├── PostgresItemRepository   ← default
├── MongoItemRepository      ← swap in as needed
└── InMemoryItemRepository   ← for testing
```

To swap databases: implement the interface, update DI binding, no route changes needed.

## Service Structure

Each Kotlin service follows this layout:
```
src/main/kotlin/com/starter/{service}/
├── Application.kt           # Entry point
├── config/DatabaseConfig.kt # DB setup & DI
├── models/                  # Domain models & DTOs
├── repository/              # Interface + implementation
├── routes/                  # HTTP endpoints
└── services/                # Business logic
```

## Adding a New Service

1. Duplicate `services/crud-service`
2. Update package namespace
3. Rename entity (replace "items")
4. Define repository interface + implementation
5. Add to `docker-compose.yml` with own database
6. Add upstream block and location route in `nginx/nginx.conf`
7. Include `auth_request` directive if routes require auth

## Branching Strategy

GitHub Flow - keep it simple:
- `main` is always deployable
- Create feature branches off main with clear naming
- Merge via PR (even solo — gives you a changelog and self-review checkpoint)

Branch naming: `{type}/{short-description}`
```
feature/auth-service
feature/nginx-gateway
feature/user-service
feature/crud-service
fix/token-validation-edge-case
chore/docker-compose-cleanup
```

## Commit Convention

Use Conventional Commits for scannable history and automated changelogs.

```
feat(auth): implement JWT issuance and refresh
feat(gateway): add rate limiting via limit_req
fix(user): handle missing X-User-Id header
refactor(crud): extract repository interface
chore: update Docker base images
docs: add API reference for auth endpoints
```

Format: `{type}({scope}): {imperative description}`

**Types:** feat, fix, refactor, chore, docs, test, ci

**Scopes:** auth, user, crud, gateway, docker, or omit for cross-cutting changes

## PR Workflow

One PR per service/feature for isolated diffs and clean merge history:
- `feature/project-scaffolding` — compose, dockerfiles, gradle setup
- `feature/auth-service`
- `feature/nginx-gateway`
- `feature/user-service`
- `feature/crud-service`

Use squash merge to keep main history clean — one commit per feature.

## Tags and Releases

Tag milestones once the stack is functional:
```
v0.1.0  — scaffolding + auth service
v0.2.0  — gateway + user service
v0.3.0  — crud service (template complete)
v1.0.0  — stable, documented, ready for reuse
```

Follow semver: `v{major}.{minor}.{patch}`
