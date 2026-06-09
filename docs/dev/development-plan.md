Implementation Plan: Bootstrap Membership Application

Context

The membership application currently has only MembershipApplication.java and minimal configuration. The full domain spec exists at docs/dev/2026-06-08-10-00_spec-membership.md with 19 user stories. This plan
bootstraps all four Spring Modulith modules (shared, member, dues, notification) with REST APIs, Flyway migrations, tests, and build tooling.

Decisions: REST API only (no Thymeleaf), no auth (domain first), local PostgreSQL (no Docker Compose), full build tooling (Spotless + JaCoCo + git-commit-id + Taskfile).

 ---
Step 1: Build Tooling & Configuration

Modify pom.xml:
- Add dependencies: hypersistence-utils-hibernate-73 (TSID), testcontainers-postgresql, taikai (ArchUnit)
- Add plugins: spotless-maven-plugin (Palantir format), jacoco-maven-plugin (80% min), git-commit-id-maven-plugin
- Update spring-boot-maven-plugin with build-info goal

Create files:
- Taskfile.yml — tasks: build, test, format, format_check, run
- src/main/resources/application-postgres.yaml — datasource config for local PostgreSQL
- src/test/resources/application-test.yaml — test profile config
- src/test/java/.../TestcontainersConfig.java — PostgreSQL Testcontainer with @ServiceConnection
- src/test/java/.../BaseIT.java — abstract base for integration tests (RestTestClient, Testcontainers)

Modify files:
- src/main/resources/application.yaml — add JPA, Flyway, Modulith event config
- src/test/java/.../MembershipApplicationTests.java — extend BaseIT

Verify: ./mvnw clean compile passes

 ---
Step 2: Shared Module

Create shared/ module (OPEN type) with:
- BaseEntity — @MappedSuperclass with createdAt, updatedAt, @Version
- IdGenerator — TSID-based string ID generation
- AssertUtil — requireNotNull, requireNotBlank helpers
- DomainEvent — marker interface for all domain events
- SpringEventPublisher — wraps ApplicationEventPublisher
- JpaConfig — @EnableJpaAuditing
- ClockConfig — provides Clock bean (testable)
- ResourceNotFoundException, DomainException, MemberAlreadyExistsException
- GlobalExceptionHandler (in root config/ package) — @RestControllerAdvice returning ProblemDetail

Tests: ModularityTest (ApplicationModules.verify()), ArchUnitTests (Taikai constraints)

Verify: ./mvnw test passes

 ---
Step 3: Member Module — Entity & Repository

Flyway migrations:
- V1__init_member_table.sql — members table with indexes
- V2__membership_number_sequence.sql — sequence counter table

Create member entity layer:
- MemberId — @Embeddable record value object (public)
- MemberStatus — enum with canTransitionTo() validation (public)
- MemberEntity — JPA entity with factory method, domain methods (package-protected)
- MemberRepository — JpaRepository with search, filter, getById (package-protected)
- MembershipNumberSequenceEntity + MembershipNumberSequenceRepository — pessimistic-lock sequence generation

Domain events (public records):
- MemberRegisteredEvent, MemberStatusChangedEvent, MemberProfileUpdatedEvent

Tests: MemberRepositoryTest — verifies persistence against Testcontainers

 ---
Step 4: Member Module — Service, Mapper & API Facade

Command/query/result records (public):
- RegisterMemberCmd, UpdateMemberProfileCmd, ChangeStatusCmd, MemberResult

Service layer (package-protected):
- MemberService — registration (with membership number generation), status changes, profile updates, event publishing
- MemberMapper — entity to result conversion

Public facade:
- MemberAPI — delegates to MemberService, exposes: registerMember, listMembers, getMember, changeStatus, updateProfile, findActiveMembers, inactivateMember

Tests: MemberServiceTest (unit, mocked deps), MembershipNumberGenerationTest (integration, concurrent access)

 ---
Step 5: Member Module — REST Layer

REST endpoints on MemberController (package-protected):
- POST /api/members — register member (201 Created)
- GET /api/members — paginated list with ?query= and ?status= filters
- GET /api/members/{memberId} — get by ID
- PATCH /api/members/{memberId}/status — change status
- PUT /api/members/{memberId}/profile — update contact details

Request/response DTOs (public):
- RegisterMemberRequest (with @Valid annotations), UpdateMemberProfileRequest, ChangeStatusRequest, MemberResponse

Tests: MemberControllerTests — full integration tests with RestTestClient, @Sql test data

 ---
Step 6: Dues Module

Flyway migration: V3__init_dues_record_table.sql — dues_records table with unique(year, member_id)

Full module structure:
- DuesRecordEntity with DuesRecordId value object (member_id stored as String FK, no cross-module JPA relationship)
- DuesService — markPaid (reactivates INACTIVE members via MemberAPI), createDuesRecord, inactivateUnpaidMembers
- DuesScheduler — @Scheduled cron for May 1 auto-inactivation
- MemberRegisteredListener — @ApplicationModuleListener creates initial dues record on registration
- DuesController — POST /api/members/{memberId}/dues/{year}/pay, GET /api/members/{memberId}/dues
- Events: DuesPaidEvent, MemberAutoInactivatedEvent
- DuesAPI — public facade

Dependency flow: dues → member (via MemberAPI, one-way). member → dues (via events only).

Tests: DuesServiceTest (unit), DuesControllerTests (integration)

 ---
Step 7: Notification Module (Stub)

Event listeners that log instead of sending email:
- MemberEventNotificationListener — handles MemberRegisteredEvent, MemberStatusChangedEvent
- DuesEventNotificationListener — handles DuesPaidEvent, MemberAutoInactivatedEvent
- NotificationAPI — empty placeholder facade

Tests: NotificationListenerTest — verifies listeners are triggered

 ---
Step 8: Integration & Polish

- MemberRegistrationE2ETest — full flow: register → verify dues created → mark paid → change status
- Run ./mvnw spotless:apply to format all code
- Run task build (./mvnw clean spotless:apply verify) — full green build
- Verify ModularityTest passes with all 4 modules
- Verify JaCoCo coverage ≥ 80% (adjust exclusions if needed for config classes)

 ---
Module Dependency Graph (no cycles)

shared/        ← all modules depend on (OPEN)
member/        → publishes events (no outbound deps)
dues/          → depends on MemberAPI; listens to member events
notification/  → listens to member + dues events

Verification

After each step: ./mvnw test
After all steps: task build (clean + spotless + verify with coverage)
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
