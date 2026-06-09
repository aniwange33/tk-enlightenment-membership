# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Taraku Enlightenment Club membership management application. Spring Boot 4.0.6 modular monolith (Spring Modulith 2.0.6) with Java 25, PostgreSQL, and Flyway migrations.

**Base package:** `com.tertech.tkenlightment.membership`
**Status:** Bootstrap phase — only `MembershipApplication.java` exists. Core modules must be created.

## Build & Run Commands

```bash
mvn clean install        # Full build
mvn test                 # Run tests
mvn spring-boot:run      # Start app on http://localhost:8080
mvn test -pl . -Dtest=ClassName#methodName  # Run a single test
```

Maven wrapper available: use `./mvnw` instead of `mvn` if Maven is not installed globally.

## Architecture

**Modular monolith** with four modules communicating via Spring Modulith application events (never direct service-to-service calls):

- **`member/`** — Registration, profile, status lifecycle (ACTIVE/INACTIVE/SUSPENDED/TERMINATED)
- **`dues/`** — Annual dues tracking; auto-inactivates unpaid members on May 1
- **`notification/`** — Email handler, listens to member and dues events
- **`shared/`** — OPEN module for cross-module value objects and exceptions

Events are defined in the **publishing** module's `domain/events/` package. Listeners live in the **receiving** module using `@ApplicationModuleListener`.

### Module Internal Structure

Each module follows this layout under `com.tertech.tkenlightment.membership.{module}/`:

```
domain/models/       # Entities, enums (@NamedInterface)
domain/events/       # ApplicationEvent subclasses
domain/services/     # Business logic (package-protected)
rest/controllers/    # REST endpoints
rest/dtos/           # Request/response payloads
{ModuleName}API.java # Public facade — only public entry point
package-info.java    # @ApplicationModule declaration
```

**Visibility:** Entities, repositories, mappers, services are **package-protected**. Only DTOs, enums, events, and the `{Module}API.java` facade are public.

### Naming Conventions

Entities: `*Entity` | Commands: `*Cmd` | Queries: `*Query` | Results: `*Result`
Request DTOs: `*Request` | Response DTOs: `*Response` | Repositories: `*Repository`
Services: `*Service` | Module facades: `*API`

## Database

PostgreSQL required. Create database with `createdb membership_db`, then configure datasource in `application.yaml`. Flyway migrations live in `src/main/resources/db/migration/` with format `V{number}__description.sql`. Never modify applied migrations — always create new ones.

JPA `ddl-auto` should be set to `validate` (Flyway owns the schema).

## Testing

- JUnit 5 + Spring Boot Test + Spring Modulith Test
- Integration tests use `@SpringBootTest` + `@Transactional`
- Module verification: `ApplicationModules.of(MembershipApplication.class).verify()` catches cyclic dependencies
- Testcontainers support available via Spring Boot test starters

## Key References

- **Domain spec:** `docs/dev/2026-06-08-10-00_spec-membership.md` — full domain model, user stories, edge cases
- **AGENTS.md** — detailed bootstrap guide, module creation steps, build order, and pitfalls
- **AI skills:** `.github/skills/` — development pattern references (spring-boot, JPA, TDD, code-quality, etc.)

## Module Build Order

When building from scratch: `shared/` → `member/` → `dues/` → `notification/`

## Key Domain Rules

- Membership number format: `TEC-YYYY-NNN` (auto-generated)
- Dues deadline: April 30. Reminder email: April 1. Auto-inactivation: May 1.
- Member email must be unique.
