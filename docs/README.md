# Spring Boot Project API

A Spring Boot REST API built with Java 21, Spring Data JPA, MySQL, and springdoc-openapi
(Swagger UI).

## Tech stack

- Java 21
- Spring Boot 4.0.8-SNAPSHOT
- Spring Data JPA + MySQL
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
- MySQL running locally (or update `.env` to point at your instance)

### Configure the database

All configuration is loaded from a local `.env` file via
`spring.config.import` in `src/main/resources/application.properties`. The file
`.env` is gitignored, so **every developer keeps their own private copy** — your
friend connects to her own database without touching shared code.

1. Create an empty MySQL database (e.g. `glasses_management`).
2. Copy the template and edit it with your own values:

   ```bash
   cp .env.example .env        # Windows: copy .env.example .env
   ```

   ```properties
   # .env  (do NOT commit this file)
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=glasses_management
   DB_USERNAME=root
   DB_PASSWORD=yourpassword

   JWT_SECRET=generate-your-own   # openssl rand -base64 32
   JWT_EXPIRATION_MINUTES=60

   ADMIN_EMAIL=admin@example.com
   ADMIN_PASSWORD=admin123
   ```

On startup, Flyway runs the migration file `src/main/resources/db/migration/V1__init_schema.sql`
and creates the tables. Hibernate now uses `ddl-auto=validate`, which means it only
checks the schema matches the entities, it does not change the database.
`AdminUserSeeder` creates the first admin from `ADMIN_EMAIL`/`ADMIN_PASSWORD`.
A fresh database needs no manual SQL.

Note: `DATABASE_MIGRATION.sql` is only for fixing an already-existing database that
has stale columns. Skip it when starting a new database.

### Run

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`, and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

### Run with Docker

This project includes a Dockerfile and a docker-compose.yml. They start the backend
and a MySQL 8.0 database together. Docker and Docker Compose must be installed.

1. Copy the env template and edit it with your own values:

   cp .env.example .env

2. Start the whole stack:

   docker compose up -x-build

The API is available at `http://localhost:8080`. On first start, Flyway creates the
schema inside the MySQL container.

Note about existing databases: if you already have a database built by the old
ddl-auto=update setup, it may still contain old columns such as branch_id and money
stored as FLOAT. Drop that database and let Flyway rebuild it, otherwise the app
will not start. New databases are created cleanly by Flyway.

### Test

```bash
./mvnw test
```

## Project structure

See [`agent_guide_ai.md`](agent_guide_ai.md) for the full folder layout and coding conventions
(controller → service → repository layering, DTOs, mappers, etc.). That file is also the shared
source of truth read by AI coding agents (Claude Code, Codex, opencode, Copilot, Antigravity).
