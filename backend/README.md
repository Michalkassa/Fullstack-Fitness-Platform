# LockedIn backend

Spring Boot 4 REST API for LockedIn. Java 25, Maven, PostgreSQL, Flyway, Spring Security.

The frontend is already built against the contract in **[../docs/API.md](../docs/API.md)**. Each endpoint
you implement makes the matching screen work.

## What's here

| Path | What it is |
|---|---|
| `pom.xml` | Web MVC, Data JPA, Security, Validation, Flyway, Actuator, PostgreSQL, Testcontainers |
| `src/main/resources/application.yml` | DB connection, CORS origins, JWT settings (all overridable by env vars) |
| `src/main/resources/db/migration/V1__init_schema.sql` | All tables (UUID primary keys, cascading deletes) |
| `src/main/resources/db/migration/V2__seed_catalog_and_programs.sql` | 34-exercise catalog, system user, 7 program templates |
| `config/SecurityConfig.java` | Stateless, CSRF off, CORS, BCrypt `PasswordEncoder`. **Every request is permitted for now.** |
| `health/HealthController.java` + test | `GET /api/health`, a worked example of the test-first loop |
| `BackendApplicationTests` | Boots the full app against a Testcontainers Postgres (skipped without Docker) |
| `TestBackendApplication` | Run the app locally against a throwaway Testcontainers Postgres |

Hibernate is set to `ddl-auto: validate`, so your `@Entity` classes must match the Flyway tables. Change the
schema with a new migration (`V3__…sql`), never by editing V1/V2 after they've run.

## Running

```bash
docker compose up db          # from the repo root: Postgres on localhost:5432
./mvnw spring-boot:run        # API on http://localhost:8080
```

Or skip the compose step and let Testcontainers provide Postgres:

```bash
./mvnw spring-boot:test-run   # runs TestBackendApplication
```

| Env var | Default |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/lockedin` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `lockedin` / `lockedin` |
| `JWT_SECRET` | dev-only placeholder, change it |
| `JWT_EXPIRATION` | `7d` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| `PORT` | `8080` |

## Testing

```bash
./mvnw test
```

Suggested layers (the original app's tests became the examples in API.md, "Business rules"):

| Layer | Use for | Tooling |
|---|---|---|
| Plain unit tests | Pure rules: 1RM, XP/levels, week keys, weekly stats, PRs, CSV escaping | JUnit + AssertJ, no Spring |
| Web slice | Controllers: status codes, JSON shape, validation → 400, 404s | `@WebMvcTest` + `MockMvc` + `@MockitoBean` services (see `HealthControllerTest`) |
| Repository slice | Custom queries (popular exercises, logs since, ownership filters) | `@DataJpaTest` + `@Import(TestcontainersConfiguration.class)` |
| Full integration | Auth flow end-to-end, the session endpoint, program copy | `@SpringBootTest` + `MockMvc` + Testcontainers |

Testcontainers needs Docker running. Without it, classes marked
`@Testcontainers(disabledWithoutDocker = true)` are skipped instead of failing.

## Suggested package layout

Package by feature, so each slice of the API lives together:

```
com.lockedin
├── config/        SecurityConfig, JWT filter, ProblemDetail exception handler
├── auth/          AuthController, AuthService, JwtService, DTOs
├── user/          User entity/repo, UserController (/api/users/me, /stats), XpService
├── bodyweight/
├── exercise/      Exercise, catalog, popular
├── log/           Log, OneRepMax calculator
├── workout/       Workout, WorkoutExercise, sessions
├── program/       templates + copy
├── nutrition/
├── analytics/     pure aggregation + controller
└── export/        CSV + PDF
```

Use Java `record`s for request/response DTOs that match the JSON in API.md, and keep entities out of
controller responses.

## TDD roadmap

The order below unlocks the UI screen by screen. Write the failing test first for each box.

### 1. Foundations
- [ ] Global `@RestControllerAdvice` that turns validation errors, not-found and conflicts into `ProblemDetail` (400 / 404 / 409) with a user-facing `detail`
- [ ] Pure rules with unit tests: `OneRepMax` (Brzycki), `Xp` (`xpForLevel`, `levelFromXp`, `levelProgress`). Test cases are in API.md

### 2. Auth (unlocks login/register)
- [ ] `User` entity + repository mapped to `users`
- [ ] `POST /api/auth/register`: 201, 400, 409
- [ ] `POST /api/auth/login`: 200, 401
- [ ] JWT issue + validate; filter populates the security context
- [ ] `SecurityConfig`: permit only `/api/auth/**`, `/api/health`, `/actuator/health`; 401 for everything else without a valid token
- [ ] `GET /api/users/me`, `GET /api/users/me/stats`

### 3. Body weight (Body Weight page, dashboard chart)
- [ ] `GET` / `POST` / `DELETE /api/bodyweights`. Test that one user can't see or delete another's rows (404)

### 4. Exercises and logs (Exercises pages)
- [ ] `GET /api/exercise-catalog`
- [ ] Exercise CRUD: `GET /api/exercises`, `GET/PUT/DELETE /api/exercises/{id}`, `POST /api/exercises`
- [ ] `GET /api/exercises/{id}/logs`, `POST /api/exercises/{id}/logs` (computes 1RM, +10 XP), `DELETE /api/logs/{id}`
- [ ] `GET /api/exercises/{id}/logs/previous?limit=n`
- [ ] `GET /api/exercises/popular`

### 5. Workouts (Workouts, Start Workout, live tracker)
- [ ] Workout CRUD with nested `exercises`
- [ ] `POST` / `DELETE /api/workouts/{id}/exercises…` (idempotent add)
- [ ] `POST /api/workouts/{id}/sessions`: transactional, all-or-nothing, XP = sets × 10 + 25

### 6. Programs
- [ ] `GET /api/programs`
- [ ] `POST /api/programs/{id}/copy`: reuse exercises by name, otherwise copy

### 7. Nutrition
- [ ] Entries `GET` / `POST` / `DELETE`
- [ ] `PUT /api/nutrition/goal` (upsert)
- [ ] `GET /api/nutrition/summary?since=`

### 8. Analytics (dashboard, Records page)
- [ ] Pure `dayKey` / `weekKey`, `weeklyStats`, `activityByDay`, `personalRecords` with the API.md examples as tests
- [ ] `GET /api/analytics/weekly`, `/activity`, `/records`

### 9. Export
- [ ] CSV builder + RFC 4180 escaping (unit tests), `GET /api/export/csv?type=`
- [ ] `GET /api/export/pdf` (e.g. OpenPDF)

### 10. Hardening
- [ ] Testcontainers integration test for the whole auth → log → analytics flow
- [ ] Optional: springdoc-openapi to serve Swagger UI from the controllers
