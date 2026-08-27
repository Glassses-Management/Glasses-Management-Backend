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

On startup, `spring.jpa.hibernate.ddl-auto=update` auto-creates the tables from
the JPA entities, and `AdminConfig` seeds the admin account from
`ADMIN_EMAIL`/`ADMIN_PASSWORD`. A fresh database needs no manual SQL.

> Note: `DATABASE_MIGRATION.sql` is only for fixing an *already-existing* database
> that has stale columns. Skip it when starting a new database.

### Run

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`, and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

### Test

```bash
./mvnw test
```

## Project structure

See [`agent_guide_ai.md`](agent_guide_ai.md) for the full folder layout and coding conventions
(controller → service → repository layering, DTOs, mappers, etc.). That file is also the shared
source of truth read by AI coding agents (Claude Code, Codex, opencode, Copilot, Antigravity).
