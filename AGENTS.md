# AGENTS.md - AI Agent Guide for Membership Project

## Quick Reference

**Tech Stack:** Spring Boot 4.0.6 (Java 25) + Spring Modulith 2.0.6 + PostgreSQL + Flyway migrations
**Build:** Maven (`mvn clean install`, `mvn test`, `mvn spring-boot:run`)
**Architecture:** Modular monolith with Spring Modulith using domain-driven design
**Base Package:** `com.tertech.tkenlightment.membership`
**Project Status:** Bootstrap phase — core modules must be created following the patterns below

---

## Architecture Overview

This is a **modular Spring Boot application** for Taraku Enlightenment Club membership management. Three core modules communicate via **Spring Modulith application events** (not direct service calls):

- **`member/`** – Member registration, profile, and status lifecycle (ACTIVE/INACTIVE/SUSPENDED/TERMINATED)
- **`dues/`** – Annual dues tracking; auto-inactivates unpaid members on May 1st
- **`notification/`** – Email event handler (event-driven, fires on member events and dues deadlines)
- **`shared/`** – OPEN module for cross-module value objects and exceptions

**Critical:** No cyclic dependencies. Modules only communicate through published `ApplicationEvent` instances and the `shared` OPEN module.

### Key Domain Concepts

- **Member**: Fields include `membershipNumber` (auto-generated `TEC-YYYY-NNN`), status, `joinDate`, etc.
- **DuesRecord**: Tracks annual dues; deadline is **April 30**. Members auto-inactivated if unpaid by May 1.
- **Dues Lifecycle**: Reminder email on April 1, overdue notice on May 1 → auto-inactivation.

See `/docs/dev/2026-06-08-10-00_spec-membership.md` for complete domain specification.

---

## Bootstrap & Getting Started

**Current Status:** Only `MembershipApplication.java` exists. Three core modules (`member/`, `dues/`, `notification/`) and the `shared/` module must be created.

### Create a Module (Step-by-Step)

For each new module (e.g., `member`, `dues`, `notification`, `shared`):

1. **Create directory structure:**
   ```
   src/main/java/com/tertech/tkenlightment/membership/{modulename}/
   ├── domain/
   │   ├── models/
   │   ├── events/
   │   └── services/
   ├── rest/
   │   ├── controllers/
   │   └── dtos/
   └── ...
   ```

2. **Create `package-info.java`** in module root:
   ```java
   @ApplicationModule(displayName = "Member Module")
   package com.tertech.tkenlightment.membership.member;
   
   import org.springframework.modulith.core.ApplicationModule;
   ```

3. **For `shared` module only** (OPEN module), use:
   ```java
   @ApplicationModule(displayName = "Shared Module")
   package com.tertech.tkenlightment.membership.shared;
   
   import org.springframework.modulith.core.ApplicationModule;
   ```
   This exposes all public types to other modules without explicit `@NamedInterface` markers.

4. **Create domain models** in `domain/models/` with `@Entity`, `@Data`, etc.
5. **Create public API facade** `{ModuleName}API.java` at module root (e.g., `MemberAPI.java`)
6. **Create event classes** in `domain/events/` if publishing events
7. **Create REST controllers** in `rest/controllers/` annotated with `@RestController`

### Recommended Build Order

1. **`shared/`** first — create common exceptions and value objects
2. **`member/`** — core domain, minimal event publishing
3. **`dues/`** — depends on member events
4. **`notification/`** — event listeners for member & dues events

---

## Package Structure Convention

Follow this pattern **strictly** within each module:

```
com.tertech.tkenlightment.membership.{modulename}/
├── domain/
│   ├── models/           # Domain entities, enums (annotate with @NamedInterface("module-models"))
│   ├── events/           # Module-specific ApplicationEvent subclasses
│   └── services/         # Domain/business logic services (internal use only)
├── rest/
│   ├── controllers/      # REST endpoints
│   └── dtos/             # Request/response payloads
├── {repos, mappers}/     # JPA repositories, mappers (package-protected)
├── {ModuleName}API.java  # Public API facade exporting only public methods
└── package-info.java     # Module metadata (with @ApplicationModule, @NamedInterface)
```

**Visibility Rules:**
- JPA entities, repositories, mappers: **package-protected** (no `public`)
- Services: **package-protected** (expose via `{Module}API.java` facade only)
- DTOs, enums, events: **public**
- `{Module}API.java`: Public facade with business method signatures

---

## Event-Driven Communication

**Only mechanism for inter-module communication:**

```java
// Publishing (e.g., in MemberAPI after registration)
context.publishEvent(new MemberRegisteredEvent(memberId, email));

// Listening (e.g., in notification module)
@Component
class MemberEventHandler {
    @ApplicationModuleListener
    void onMemberRegistered(MemberRegisteredEvent event) {
        // Send welcome email
    }
}
```

Define events in the **publishing module's `domain/events/`**. Listeners in **receiving module**.

---

## Testing Patterns

- **Unit tests**: Single service in isolation (e.g., `MemberServiceTest`)
- **Integration tests**: Use `@SpringBootTest` + `@Transactional`; Spring Modulith test starter includes event publishing mocks
- **Module verification**: Create `ModularityTest` with `ApplicationModules.verify()` and `Documenter` to validate architecture

Example test setup:
```java
@SpringBootTest
class MemberRegistrationTests {
    @Autowired MemberAPI memberAPI;
    @Autowired MemberRepository memberRepository;
    @Transactional
    @Test void testRegisterMember() { ... }
}
```

---

## Build & Run Commands

- **Clean build:** `mvn clean install`
- **Run app:** `mvn spring-boot:run` (starts on `http://localhost:8080`)
- **Run tests:** `mvn test`
- **Package:** `mvn package` (creates JAR in `target/`)
- **Check modularity:** Add test with `ApplicationModules.of(MembershipApplication.class).verify()`

### Database Setup

**PostgreSQL required.** Before running the application:

1. Create database: `createdb membership_db`
2. Configure in `application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/membership_db
       username: postgres
       password: <your_password>
       driver-class-name: org.postgresql.Driver
     jpa:
       hibernate:
         ddl-auto: validate
     flyway:
       enabled: true
   ```
3. Run application — Flyway migrations auto-execute on startup

---

## Flyway Migrations

- Location: `src/main/resources/db/migration/`
- Format: `V{number}__description.sql` (e.g., `V1__init_member_table.sql`)
- Flyway auto-runs on startup; **never delete or modify applied migrations**
- Create new migrations for schema changes

---

## Key Files & Where Things Live

| Responsibility | Location |
|---|---|
| Domain models | `src/main/java/.../membership/{module}/domain/models/` |
| REST endpoints | `src/main/java/.../membership/{module}/rest/controllers/` |
| Public API | `src/main/java/.../membership/{module}/{ModuleName}API.java` |
| Database schema | `src/main/resources/db/migration/` |
| Config | `src/main/resources/application.yaml` |
| Tests | `src/test/java/.../membership/` |
| Spec/docs | `/docs/dev/` |

---

## Lombok Usage

Project uses Lombok (`@Data`, `@AllArgsConstructor`, etc.). IDE annotation processors configured in `pom.xml`. When adding new entities, use `@Data` for boilerplate.

---

## IDE Setup (IntelliJ/JetBrains)

For optimal Spring Modulith development:

- **Enable annotation processing:** Settings → Compiler → Annotation Processors → Enable annotation processing
- **Lombok plugin:** Install JetBrains Lombok plugin (built-in as of 2023.1+)
- **Module detection:** After creating `package-info.java` files, rebuild project (`Build → Rebuild Project`) for IntelliJ to recognize modules
- **Event publishing:** Use IntelliJ's "Find Usages" on event classes to trace listeners across module boundaries
- **Circular dependency checker:** Run `mvn verify` to catch module import cycles before they cause issues

---

## Common Pitfalls to Avoid

1. **Direct service-to-service calls** between modules → Use published events instead
2. **Exposing internal repos/services** as public → Wrap in `{Module}API.java` facade
3. **Forgetting `package-info.java`** in shared module → Makes `@OPEN` module declaration invalid
4. **Cyclic event dependencies** → A publishes event B listens to, but B also publishes event A listens to → Refactor or use async handlers
5. **Modifying applied Flyway migrations** → Always create new migrations

---

## Useful Spring Modulith Docs

- Module verification & modularity tests: See commented-out test patterns in `.claude/skills/spring-boot/references/spring-modulith.md`
- NamedInterface exports: Mark domain models with `@NamedInterface("module-models")` for explicit cross-module visibility
- Event lifecycle: Use `@ApplicationModuleListener` with soft timeouts for long-running handlers

