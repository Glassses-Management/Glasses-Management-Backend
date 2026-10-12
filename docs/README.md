# Spring Boot Project API

A Spring Boot REST API built with Java 21, Spring Data JPA, PostgreSQL, and springdoc-openapi
(Swagger UI). The database is hosted on Aiven.

## Tech stack

- Java 21
- Spring Boot 4.0.8-SNAPSHOT
- Spring Data JPA + PostgreSQL
- Bean Validation (jakarta.validation)
- springdoc-openapi (Swagger UI)
- Lombok

## Getting started

Clone the repository, then detach it from this repo's git history so you can start your own:

```bash
git clone <repo-url> spring_boot_project_api
cd spring_boot_project_api
rm -rf .git
git init
```

### Prerequisites

- JDK 21
- PostgreSQL: an Aiven service, or a local database via `docker compose up -d postgres`

### Configure the database

All configuration is loaded from a local `.env` file via
`spring.config.import` in `src/main/resources/application.properties`. The file
`.env` is gitignored, so **every developer keeps their own private copy** — your
friend connects to her own database without touching shared code.

1. Create an empty PostgreSQL database (e.g. on Aiven), or let `docker compose` create one.
2. Copy the template and edit it with your own values:

   ```bash
   cp .env.example .env        # Windows: copy .env.example .env
   ```

   ```properties
   # .env  (do NOT commit this file)
   DB_HOST=your-aiven-host     # e.g. pg-xxxx.aivencloud.com
   DB_PORT=5432
   DB_NAME=glasses_management  # your Aiven database name
   DB_USERNAME=avn_userxxxx
   DB_PASSWORD=yourpassword
   DB_SSLMODE=require          # Aiven refuses plaintext connections

   JWT_SECRET=generate-your-own   # openssl rand -base64 32
   JWT_EXPIRATION_MINUTES=60

   ADMIN_EMAIL=admin@example.com
   ADMIN_PASSWORD=admin123
   ```

On startup, Flyway runs the migration files in `src/main/resources/db/migration/`
and creates the tables. Hibernate uses `ddl-auto=validate`, which means it only
checks the schema matches the entities, it does not change the database.
`AdminUserSeeder` creates the first admin from `ADMIN_EMAIL`/`ADMIN_PASSWORD`.
A fresh database needs no manual SQL.

Two Flyway modes, because the schema has to work both for an existing database
and for a brand new one:

- **An existing database** (one that already has data, e.g. moved over from MySQL)
  is the default. Flyway baselines it at V10 so it never tries to recreate tables
  that are already present.
- **A brand new empty database** must actually run V1–V10. Start it with the
  `local` profile so baselining is skipped:

  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
  ```

  The `app` service in `docker-compose.yml` already sets this profile.

### Run

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`, and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

### Run with Docker

This project includes a Dockerfile and a docker-compose.yml. They start the backend
and a PostgreSQL 16 database together. Docker and Docker Compose must be installed.

1. Copy the env template and edit it with your own values:

   cp .env.example .env

2. Start the whole stack:

   docker compose up -x-build

The API is available at `http://localhost:8080`, and pgadmin at `http://localhost:8081`.
On first start, Flyway creates the schema inside the PostgreSQL container.

### Test

```bash
./mvnw test
```

## Project structure

See [`AGENTS.md`](AGENTS.md) for the full folder layout and coding conventions
(controller → service → repository layering, DTOs, mappers, etc.). That file is also the shared
source of truth read by AI coding agents (Claude Code, Codex, opencode, Copilot, Antigravity).

Database setup and hosting are documented in [`AIVEN_SETUP.md`](AIVEN_SETUP.md).
