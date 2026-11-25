# LockedIn REST API contract

This is the contract between the React frontend (`frontend/`) and the Spring Boot backend (`backend/`).
The frontend is already built against it: every endpoint below is called from
[`frontend/src/api/endpoints.ts`](../frontend/src/api/endpoints.ts), and the TypeScript shapes live in
[`frontend/src/api/types.ts`](../frontend/src/api/types.ts). Implement each endpoint in Spring Boot to
match, and the matching screen starts working.

If you change a shape or path, update this file, `types.ts` and `endpoints.ts` together.

- [Conventions](#conventions)
- [Authentication](#authentication)
- [Endpoint summary](#endpoint-summary)
- [Auth & users](#auth--users)
- [Body weight](#body-weight)
- [Exercises](#exercises)
- [Exercise catalog](#exercise-catalog)
- [Logs](#logs)
- [Workouts](#workouts)
- [Programs](#programs)
- [Nutrition](#nutrition)
- [Analytics](#analytics)
- [Export](#export)
- [Business rules](#business-rules)
- [Validation rules](#validation-rules)

---

## Conventions

| Topic | Rule |
|---|---|
| Base path | Everything is under `/api`. In dev, Vite proxies `/api` to `http://localhost:8080`. |
| Format | JSON request and response bodies, `Content-Type: application/json`. |
| IDs | UUID strings, e.g. `"3f1c2b9e-4d5a-4e0b-9a51-7d1f0c2e8b11"`. |
| Timestamps | ISO-8601 instants in UTC, e.g. `"2026-06-11T13:45:00Z"` (Java `Instant`). |
| Dates | `"YYYY-MM-DD"` strings (Java `LocalDate`), used by analytics. |
| Units | Weights in kilograms, macros in grams, energy in kcal. |
| Ownership | Every resource belongs to the authenticated user. A request for another user's resource returns **404**, not 403, so ids don't leak. |
| Ordering | Lists are sorted as noted per endpoint. "oldest first" means `createdAt` ascending. |
| Created | `POST` that creates something returns **201** with the created object. |
| Deleted | `DELETE` returns **204** with no body. |

### Errors

Return [RFC 7807 problem details](https://www.rfc-editor.org/rfc/rfc7807). Spring's `ProblemDetail`
does this for you. The frontend shows `detail` to the user, so write it as a user-facing sentence.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Enter a weight and the reps",
  "errors": { "reps": "must be greater than 0" }
}
```

`errors` (field → message) is optional. If `detail` is missing, the frontend falls back to the first
value in `errors`, then `title`.

| Status | When |
|---|---|
| 400 | Validation failed |
| 401 | Missing, invalid or expired token; wrong email/password on login |
| 404 | Resource doesn't exist or isn't yours |
| 409 | Conflict, e.g. registering an email that's already used |
| 501 | Optional: return this from endpoints you haven't built yet. The UI shows "This endpoint isn't implemented yet." |

---

## Authentication

Stateless JWT bearer tokens.

1. `POST /api/auth/register` or `POST /api/auth/login` returns `{ token, user }`.
2. The frontend stores the token in `localStorage` and sends it on every request:
   `Authorization: Bearer <token>`
3. If any request comes back **401**, the frontend clears the token and sends the user to `/login`.

Suggested token: HS256, `sub` = user id, `exp` = 7 days (`app.jwt.secret` / `app.jwt.expiration` in
`application.yml`). `spring-boot-starter-oauth2-resource-server` can validate it for you, or you can write
a `OncePerRequestFilter`.

Until auth is done, [`SecurityConfig`](../backend/src/main/java/com/lockedin/config/SecurityConfig.java)
permits every request. Lock it down when the JWT filter exists: only `/api/auth/**`, `/api/health` and
`/actuator/health` should be public.

---

## Endpoint summary

All endpoints except `/api/auth/*` require a bearer token.

| Method | Path | Purpose | Success |
|---|---|---|---|
| POST | `/api/auth/register` | Create account, return token | 201 `AuthResponse` |
| POST | `/api/auth/login` | Log in, return token | 200 `AuthResponse` |
| GET | `/api/users/me` | Current user | 200 `User` |
| GET | `/api/users/me/stats` | XP, level and progress | 200 `UserStats` |
| GET | `/api/bodyweights` | List body weights, oldest first | 200 `BodyWeight[]` |
| POST | `/api/bodyweights` | Add a body weight | 201 `BodyWeight` |
| DELETE | `/api/bodyweights/{id}` | Delete a body weight | 204 |
| GET | `/api/exercises` | List my exercises | 200 `Exercise[]` |
| GET | `/api/exercises/popular?limit=5` | Most-logged exercises | 200 `PopularExercise[]` |
| GET | `/api/exercises/{id}` | One exercise | 200 `Exercise` |
| POST | `/api/exercises` | Create an exercise | 201 `Exercise` |
| PUT | `/api/exercises/{id}` | Rename / edit description | 200 `Exercise` |
| DELETE | `/api/exercises/{id}` | Delete exercise and its logs | 204 |
| GET | `/api/exercise-catalog` | Global exercise library | 200 `CatalogExercise[]` |
| GET | `/api/exercises/{id}/logs` | Logs for an exercise, oldest first | 200 `Log[]` |
| GET | `/api/exercises/{id}/logs/previous?limit=n` | Most recent n logs, newest first | 200 `Log[]` |
| POST | `/api/exercises/{id}/logs` | Log one set | 201 `Log` |
| DELETE | `/api/logs/{id}` | Delete a log | 204 |
| GET | `/api/workouts` | List my workouts | 200 `Workout[]` |
| GET | `/api/workouts/{id}` | One workout | 200 `Workout` |
| POST | `/api/workouts` | Create a workout | 201 `Workout` |
| PUT | `/api/workouts/{id}` | Rename / edit description | 200 `Workout` |
| DELETE | `/api/workouts/{id}` | Delete a workout | 204 |
| POST | `/api/workouts/{id}/exercises` | Add an exercise to a workout | 201 / 200 `WorkoutExercise` |
| DELETE | `/api/workouts/{id}/exercises/{workoutExerciseId}` | Remove it | 204 |
| POST | `/api/workouts/{id}/sessions` | Finish a live workout: save all sets, award XP | 201 `WorkoutSessionResult` |
| GET | `/api/programs` | Public program templates | 200 `Program[]` |
| POST | `/api/programs/{id}/copy` | Copy a program into my workouts | 201 `Workout` |
| GET | `/api/nutrition/entries` | List nutrition entries, oldest first | 200 `NutritionEntry[]` |
| POST | `/api/nutrition/entries` | Log an entry | 201 `NutritionEntry` |
| DELETE | `/api/nutrition/entries/{id}` | Delete an entry | 204 |
| PUT | `/api/nutrition/goal` | Create or replace my daily goal | 200 `NutritionGoal` |
| GET | `/api/nutrition/summary?since=…` | Goal + what I've eaten since `since` | 200 `NutritionSummary` |
| GET | `/api/analytics/weekly` | Weekly volume and sessions | 200 `WeeklyStat[]` |
| GET | `/api/analytics/activity` | Sets per day (heatmap) | 200 `ActivityDay[]` |
| GET | `/api/analytics/records` | Personal records per exercise | 200 `PersonalRecord[]` |
| GET | `/api/export/csv?type=logs\|bodyweight\|nutrition` | CSV download | 200 `text/csv` |
| GET | `/api/export/pdf` | PDF progress report | 200 `application/pdf` |

---

## Auth & users

### `POST /api/auth/register`

```json
{ "email": "lifter@example.com", "password": "correct-horse" }
```

The frontend checks "confirm password" itself; it isn't sent.

**201**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9…",
  "user": { "id": "…", "email": "lifter@example.com", "name": null, "image": null, "xp": 0 }
}
```

- 400 invalid email / password too short
- 409 `"An account with this email already exists"`

Store the password with `PasswordEncoder` (BCrypt bean already defined). Normalise email to lower case.

### `POST /api/auth/login`

```json
{ "email": "lifter@example.com", "password": "correct-horse" }
```

**200** same shape as register. **401** `"Email or password is incorrect"`. Use the same message whether
the email or the password is wrong.

### `GET /api/users/me`

**200** `User`
```json
{ "id": "…", "email": "lifter@example.com", "name": null, "image": null, "xp": 140 }
```

### `GET /api/users/me/stats`

**200** `UserStats`, computed from `xp` (see [XP and levels](#xp-and-levels)).
```json
{ "xp": 150, "level": 2, "current": 50, "needed": 200, "percent": 25 }
```

---

## Body weight

`BodyWeight`
```json
{ "id": "…", "weight": 82.5, "createdAt": "2026-06-11T08:00:00Z" }
```

| Endpoint | Notes |
|---|---|
| `GET /api/bodyweights` | Oldest first. The frontend computes highest/lowest/average and 7/30/365-day change. |
| `POST /api/bodyweights` | Body `{ "weight": 82.5 }`. `weight` > 0, otherwise 400 `"Please enter a weight"`. |
| `DELETE /api/bodyweights/{id}` | 204, or 404 if it's not yours. |

---

## Exercises

`Exercise`
```json
{
  "id": "…",
  "name": "Barbell Bench Press",
  "description": "Lie on a flat bench, lower the bar to mid-chest, press up to lockout.",
  "muscleGroup": "Chest",
  "equipment": "Barbell",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

| Endpoint | Notes |
|---|---|
| `GET /api/exercises` | My exercises, newest first. |
| `GET /api/exercises/popular?limit=5` | My exercises ordered by number of logs, descending; `limit` defaults to 5. Shape: `{ "id", "name", "logCount" }`. |
| `GET /api/exercises/{id}` | 404 if missing or not mine. |
| `POST /api/exercises` | Body `{ name, description, muscleGroup?, equipment? }`. `muscleGroup`/`equipment` are filled in when the user picks from the catalog; they may be `null`. |
| `PUT /api/exercises/{id}` | Body `{ name, description }`. Returns the updated exercise. |
| `DELETE /api/exercises/{id}` | Also deletes its logs and removes it from workouts (DB cascades do this). |

---

## Exercise catalog

### `GET /api/exercise-catalog`

The global, read-only library (seeded by Flyway `V2`). Sort by `muscleGroup`, then `name`. Not
user-scoped.

```json
[{ "id": "…", "name": "Deadlift", "muscleGroup": "Back", "equipment": "Barbell", "instructions": "Hinge at the hips…" }]
```

---

## Logs

A log is one set of an exercise. `Log`
```json
{ "id": "…", "exerciseId": "…", "weight": 100, "reps": 5, "oneRepMax": 112, "createdAt": "2026-06-11T18:02:00Z" }
```

| Endpoint | Notes |
|---|---|
| `GET /api/exercises/{id}/logs` | Oldest first (drives the 1RM chart). 404 if the exercise isn't mine. |
| `GET /api/exercises/{id}/logs/previous?limit=n` | The **n most recent** logs, **newest first**. The live workout screen shows them in a "Previous" column. Empty list if none. |
| `POST /api/exercises/{id}/logs` | Body `{ "weight": 100, "reps": 5 }`. Server computes `oneRepMax` and awards `XP_PER_SET` XP. |
| `DELETE /api/logs/{id}` | 404 if not mine. Doesn't take XP back. |

---

## Workouts

`Workout`. `exercises` is in the order they were added. In each item, `id` is the **link** id
(the `workout_exercises` row), used to remove it; `exerciseId` is the exercise.

```json
{
  "id": "…",
  "name": "Push Day",
  "description": "Chest, shoulders, triceps",
  "createdAt": "2026-06-01T10:00:00Z",
  "exercises": [
    { "id": "link-uuid", "exerciseId": "exercise-uuid", "name": "Barbell Bench Press" }
  ]
}
```

| Endpoint | Notes |
|---|---|
| `GET /api/workouts` | My workouts (not templates), newest first, each with its exercises. Watch for N+1 queries. |
| `GET /api/workouts/{id}` | 404 if not mine. |
| `POST /api/workouts` | Body `{ name, description }`. Returns it with `exercises: []`. |
| `PUT /api/workouts/{id}` | Body `{ name, description }`. |
| `DELETE /api/workouts/{id}` | Deletes the workout and its links, not the exercises. |
| `POST /api/workouts/{id}/exercises` | Body `{ "exerciseId": "…" }`. The exercise must be mine (404 otherwise). If it's already in the workout, return the existing link with **200**; otherwise **201**. |
| `DELETE /api/workouts/{id}/exercises/{workoutExerciseId}` | 404 if the link isn't in this (my) workout. |

### `POST /api/workouts/{id}/sessions`

Called when the user presses **Finish workout** on the live tracker. Saves every set in one
transaction and awards XP.

```json
{
  "exercises": [
    { "exerciseId": "…", "sets": [{ "weight": 100, "reps": 5 }, { "weight": 100, "reps": 5 }] },
    { "exerciseId": "…", "sets": [{ "weight": 60, "reps": 8 }] }
  ]
}
```

**201**
```json
{ "logsCreated": 3, "xpAwarded": 55 }
```

- Each set becomes a `Log` (with `oneRepMax` computed).
- `xpAwarded = logsCreated × XP_PER_SET + XP_PER_WORKOUT` (3 × 10 + 25 = 55).
- 400 if `exercises` is empty or any set is invalid. 404 if the workout or any exercise isn't mine.
  Nothing is saved on error.

---

## Programs

Public templates (PPL, StrongLifts 5×5, Starting Strength) seeded by Flyway `V2`. They're `workouts` rows
with `is_template = true AND is_public = true`, owned by the system user.

| Endpoint | Notes |
|---|---|
| `GET /api/programs` | Same shape as `Workout`, sorted by name. |
| `POST /api/programs/{id}/copy` | Creates a new workout for me with the same name, description and exercises. For each template exercise, reuse my exercise with the same name if I have one, otherwise create my own copy (keeping description, muscle group, equipment). Returns the new `Workout` (**201**). 404 if not a public template. |

---

## Nutrition

`Macros` = `{ "calories": 650, "protein": 40, "carbs": 70, "fat": 20 }` (calories is an integer).

| Endpoint | Notes |
|---|---|
| `GET /api/nutrition/entries` | Oldest first. Each entry is `Macros` + `id` + `createdAt`. |
| `POST /api/nutrition/entries` | Body `Macros`. Macros default to 0 when omitted. |
| `DELETE /api/nutrition/entries/{id}` | 204 / 404. |
| `PUT /api/nutrition/goal` | Body `Macros`. Upsert: one goal per user. Returns the goal. |
| `GET /api/nutrition/summary?since=<instant>` | See below. |

### `GET /api/nutrition/summary?since=2026-06-11T22:00:00Z`

`since` is the start of the user's **local** day, sent by the browser so "today" follows their timezone.
If it's missing, use the start of the current UTC day.

```json
{
  "goal": { "calories": 2500, "protein": 180, "carbs": 250, "fat": 80 },
  "consumed": { "calories": 1200, "protein": 90, "carbs": 110, "fat": 40 }
}
```

`goal` is `null` if none is set. `consumed` sums entries with `createdAt >= since` and is all zeros if
there are none.

---

## Analytics

All computed from the current user's logs. These rules are good candidates for pure, unit-tested
service classes. See [Business rules](#business-rules) for the exact definitions and test cases.

| Endpoint | Shape |
|---|---|
| `GET /api/analytics/weekly` | `[{ "week": "2026-06-08", "volume": 1400, "sessions": 2 }]`, oldest week first |
| `GET /api/analytics/activity` | `[{ "date": "2026-06-08", "count": 2 }]`, any order, only days with sets |
| `GET /api/analytics/records` | `[{ "exerciseId", "exerciseName", "bestWeight", "bestOneRepMax", "bestVolume", "achievedAt" }]`, sorted by `exerciseName` |

---

## Export

The frontend downloads these with `fetch` (so the bearer token is sent) and saves the response body.
Set `Content-Disposition` so the file gets a sensible name. `SecurityConfig` already exposes that header
to the browser.

### `GET /api/export/csv?type=logs|bodyweight|nutrition`

`type` defaults to `logs`. Response `text/csv; charset=utf-8`,
`Content-Disposition: attachment; filename="lockedin-<type>.csv"`. Rows are oldest first. Dates are
`YYYY-MM-DD`. Quote fields per RFC 4180: wrap in `"` when a value contains `,`, `"`, CR or LF, and
double any `"` inside.

| type | Header row |
|---|---|
| `logs` | `date,exercise,weight_kg,reps,est_1rm_kg` |
| `bodyweight` | `date,weight_kg` |
| `nutrition` | `date,calories,protein_g,carbs_g,fat_g` |

### `GET /api/export/pdf`

`application/pdf`, `Content-Disposition: attachment; filename="lockedin-progress-report.pdf"`.
A one-page summary: user email, generation date, total sets, total volume, first and latest body
weight, and the personal records table. (OpenPDF or Apache PDFBox both work.)

---

## Business rules

These come from the original app and its Vitest suite. Each example is a ready-made test case.

### Estimated one-rep max (Brzycki)

```
oneRepMax = floor(weight / (1.0278 - 0.0278 × reps))
```

| weight | reps | 1RM |
|---|---|---|
| 100 | 1 | 100 |
| 0 | 5 | 0 |
| 100 | 5 | 112 |
| 100 | 10 | 133 |

More reps at the same weight gives a higher estimate. The result is always an integer.

### XP and levels

- `XP_PER_SET = 10` for every logged set (single log or session).
- `XP_PER_WORKOUT = 25` bonus per finished session.
- XP needed to *reach* level n: `xpForLevel(n) = 50 × (n − 1) × n` (level 1 = 0, 2 = 100, 3 = 300, 4 = 600).
- `level` is the highest n with `xpForLevel(n) <= xp`. Negative XP counts as level 1.
- `current = xp − xpForLevel(level)`, `needed = xpForLevel(level + 1) − xpForLevel(level)`,
  `percent = round(current / needed × 100)`.

| xp | level | current | needed | percent |
|---|---|---|---|---|
| 0 | 1 | 0 | 100 | 0 |
| 99 | 1 | 99 | 100 | 99 |
| 100 | 2 | 0 | 200 | 0 |
| 150 | 2 | 50 | 200 | 25 |
| 299 | 2 | 199 | 200 | 100 |
| 300 | 3 | 0 | 300 | 0 |
| 600 | 4 | 0 | 400 | 0 |

### Day and week keys (UTC)

- Day key: the UTC date of `createdAt`. `2026-06-11T13:45:00Z` → `2026-06-11`.
- Week key: the Monday (UTC) starting that week. Thursday `2026-06-11` → `2026-06-08`;
  Sunday `2026-06-14` → `2026-06-08`; Monday `2026-06-08T23:59Z` → `2026-06-08`.

### Weekly stats

Group logs by week key. `volume` = Σ weight × reps; `sessions` = number of distinct day keys. Sort
oldest first. No logs → `[]`.

| createdAt | weight × reps |
|---|---|
| 2026-06-08T10:00Z (Mon) | 100 × 5 |
| 2026-06-08T11:00Z (Mon) | 50 × 10 |
| 2026-06-10T10:00Z (Wed) | 80 × 5 |
| 2026-06-15T10:00Z (next Mon) | 60 × 5 |

→ `[{ week: "2026-06-08", volume: 1400, sessions: 2 }, { week: "2026-06-15", volume: 300, sessions: 1 }]`

### Activity

Count logs per day key. Three logs on `2026-06-08` (two) and `2026-06-09` (one) →
`[{ date: "2026-06-08", count: 2 }, { date: "2026-06-09", count: 1 }]`.

### Personal records

Per exercise:
- `bestWeight` = max weight
- `bestOneRepMax` = max oneRepMax; `achievedAt` = day key of the set that set it (first one wins ties)
- `bestVolume` = max single-set weight × reps

Sort by exercise name. Example: Bench logs `80×5 (1RM 90, 2026-06-01)` and `100×3 (1RM 109, 2026-06-08)`
→ `bestWeight 100, bestOneRepMax 109, bestVolume 400` (80×5 beats 100×3), `achievedAt 2026-06-08`.

---

## Validation rules

The frontend checks these before sending, but the backend must enforce them too. The messages are what
the UI shows if the server rejects a request.

| Field | Rule | Message |
|---|---|---|
| email | required, valid email | `Please enter an email` / `Not a valid email` |
| password (register) | at least 8 characters | `Password must be at least 8 characters` |
| exercise / workout `name` | required, 1–40 chars after trimming | `Enter a name and a description` / `Name must be at most 40 characters` |
| exercise / workout `description` | required, not blank | `Enter a name and a description` |
| body weight `weight` | > 0 | `Please enter a weight` |
| log / set `weight` | >= 0 | `Enter a weight and the reps` |
| log / set `reps` | integer > 0 | `Enter a weight and the reps` |
| nutrition `calories` | required integer >= 0 | `Enter a calorie amount` / `Calories cannot be negative` |
| nutrition `protein`/`carbs`/`fat` | >= 0, default 0 | `Macros cannot be negative` |

Changed from the original Next.js app: names can be up to 40 characters (was 15, which cut off catalog
names like "Incline Dumbbell Press"); minimum password length is 8 (was 5); set weight may be 0 for
bodyweight exercises.
