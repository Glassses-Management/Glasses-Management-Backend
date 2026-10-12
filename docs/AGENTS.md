# AGENTS.md

Instructions for AI coding agents working on this repository.

## Project Overview

Backend REST API for an **Optical Shop Management System**. Manages customers,
staff users, products, inventory, appointments, prescriptions, and orders.
See `agent_guide_ai.md` for the business workflow diagrams.

## Tech Stack

- **Java 21**, **Spring Boot 4.x** (`spring-boot-starter-parent`)
- Spring Data JPA + **PostgreSQL** (`postgresql`), hosted on **Aiven**
- Bean Validation (`spring-boot-starter-validation`)
- **Lombok** (configured via `annotationProcessorPaths` in `pom.xml`)
- springdoc-openapi (Swagger UI)

## Commands (Windows PowerShell)

```powershell
./mvnw compile -q        # compile check — run this after every change
./mvnw test              # run tests
./mvnw spring-boot:run   # start the app
```

Always verify changes with `./mvnw compile -q` before finishing a task.

## Package Layout

Base package: `com.example.spring_boot_project_api`

```
src/main/java/com/example/spring_boot_project_api/
├── model/          # JPA entities (User, Customer, Product, etc.)
├── dto/
│   ├── request/    # incoming payloads, bean-validation annotated
│   └── response/   # outgoing payloads, never expose secrets
├── mapper/         # pure converters entity <-> DTO
├── repository/     # Spring Data JpaRepository interfaces
├── service/        # business logic, transaction boundaries
└── controller/     # REST endpoints
```

## Conventions

### Layer responsibilities
- **Controller**: HTTP mapping, validation trigger, calls service.
- **Service**: owns business logic, transactions, and **entity relation lookups**
  (e.g. resolve `manager_id` from a request into a `User` via repository,
  throw `EntityNotFoundException` when missing).
- **Mapper**: **pure field copying only** — no repository injection, no DB access,
  no exception throwing. Request → entity maps scalar fields; entity → response is null-safe.
- **Repository**: Spring Data interfaces only.

### Entities
- Lombok `@Data`, `@Entity`, `@Table(name = "snake_case")`.
- `Long` IDs with `GenerationType.IDENTITY`.
- Relations: `@ManyToOne(fetch = FetchType.LAZY)` with `@JoinColumn(name = "<fk_column>")`.

### DTOs
- Named `XRequest` / `XResponse` (watch spelling: "Response", not "Respone").
- Annotate request DTOs with validation messages; every DTO needs Lombok `@Data`
  or it silently has no getters/setters.
- Relation references are plain `Long` id fields (e.g. `manager_id`), matching the
  entity PK type — never put entities inside DTOs.
- Responses must NOT include `password_hash`.

### Naming
- Getters/setters are Lombok-generated from field names: field `manager_id` →
  `getManager_id()` / `setManager_id(...)`. A `User manager` field gives
  `getManager()`, not `getManager_id()`.
- Repositories: `XRepository extends JpaRepository<X, Long>`.

## Known Quirks

- Some entities carry unused Hibernate imports (`ManyToAny`, `FetchClauseType`) left over
  from IDE auto-imports.
