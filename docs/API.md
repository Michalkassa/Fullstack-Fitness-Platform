# LockedIn REST API

This document lists **every endpoint the React frontend needs from the Spring Boot backend**: 38 in total.
The frontend is already built against it. Each endpoint is called from
[`frontend/src/api/endpoints.ts`](../frontend/src/api/endpoints.ts), and the TypeScript shapes are in
[`frontend/src/api/types.ts`](../frontend/src/api/types.ts). When you implement an endpoint to match its
entry here, the screens listed under **Used by** start working.

If you change a path or a shape, update this file, `types.ts` and `endpoints.ts` together.

## Contents

- [Conventions](#conventions)
- [Authentication](#authentication)
- [Endpoint index](#endpoint-index)
- [Data types](#data-types)
- Endpoint reference
  - [Auth](#auth)
  - [Users](#users)
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
- [Endpoints by screen](#endpoints-by-screen)

---

## Conventions

| Topic | Rule |
|---|---|
| Base path | Everything is under `/api`. In development, Vite proxies `/api` to `http://localhost:8080`; in Docker, nginx does. |
| Format | JSON request and response bodies (`Content-Type: application/json`), except the CSV/PDF exports. |
| IDs | UUID strings, e.g. `"3f1c2b9e-4d5a-4e0b-9a51-7d1f0c2e8b11"`. |
| Timestamps | ISO-8601 instants in UTC, e.g. `"2026-06-11T13:45:00Z"` (Java `Instant`). |
| Dates | `"YYYY-MM-DD"` strings (Java `LocalDate`). Used by analytics. |
| Units | Weights in kilograms, macros in grams, energy in kcal. |
| Ownership | Every resource belongs to the signed-in user. Asking for another user's resource returns **404**, not 403, so ids don't leak. |
| Ordering | Each endpoint states its sort order. "Oldest first" means `createdAt` ascending. |
| Create | A `POST` that creates something returns **201** with the created object. |
| Delete | A `DELETE` returns **204** with no body. |
| Nulls | Optional fields are sent as `null`, never left out. |

### Errors

Errors use [RFC 7807 problem details](https://www.rfc-editor.org/rfc/rfc7807); Spring's `ProblemDetail`
produces this format. The frontend shows `detail` to the user, so write it as a sentence a user can read.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Enter a weight and the reps",
  "errors": { "reps": "must be greater than 0" }
}
```

`errors` (field → message) is optional. If there's no `detail`, the frontend shows the first value in
`errors`, then `title`.

| Status | Meaning | What the frontend does |
|---|---|---|
| 400 | Validation failed | Shows `detail` next to the form |
| 401 | Missing, invalid or expired token; wrong email or password on login | Clears the token and sends the user to `/login` (shows `detail` on the login form) |
| 404 | Doesn't exist, or belongs to someone else | Shows `detail` inline |
| 409 | Conflict, e.g. the email is already registered | Shows `detail` |
| 501 | Optional: return this from endpoints you haven't built yet | Shows "This endpoint isn't implemented yet." |
| 5xx | Server error | Shows "Something went wrong on the server." |

The frontend doesn't retry 4xx/5xx responses, so an unfinished endpoint fails once and the rest of the page
keeps working.

---

## Authentication

Requests are authenticated with a stateless JWT bearer token.

1. `POST /api/auth/register` or `POST /api/auth/login` returns `{ token, user }`.
2. The frontend stores the token in `localStorage` (key `lockedin.token`) and sends it on every request
   as `Authorization: Bearer <token>`.
3. If any request with a token comes back **401**, the frontend deletes the token and redirects to `/login`.
4. Logging out happens only in the frontend: it deletes the token. No logout endpoint is needed.

Suggested token: HS256, `sub` = user id, `exp` = 7 days, signed with `app.jwt.secret` and expiring after
`app.jwt.expiration` (both in `application.yml`). `spring-boot-starter-oauth2-resource-server` can
validate it, or you can write a `OncePerRequestFilter`.

**Public endpoints:** `POST /api/auth/register`, `POST /api/auth/login`, plus `/api/health` and
`/actuator/health`. **Every other endpoint requires a valid token** and returns 401 without one.

[`SecurityConfig`](../backend/src/main/java/com/lockedin/config/SecurityConfig.java) enforces this. A
controller gets the signed-in user's id with a `@CurrentUser UUID userId` parameter. Public endpoints ignore
the `Authorization` header, so a stale token left in the browser can't block logging in again.

---

## Endpoint index

| # | Method | Path | Auth | Success | Used by |
|---|---|---|---|---|---|
| 1 | POST | [`/api/auth/register`](#post-apiauthregister) | public | 201 `AuthResponse` | Register |
| 2 | POST | [`/api/auth/login`](#post-apiauthlogin) | public | 200 `AuthResponse` | Login |
| 3 | GET | [`/api/users/me`](#get-apiusersme) | token | 200 `User` | Sidebar (on every dashboard page) |
| 4 | GET | [`/api/users/me/stats`](#get-apiusersmestats) | token | 200 `UserStats` | Sidebar level badge |
| 5 | GET | [`/api/bodyweights`](#get-apibodyweights) | token | 200 `BodyWeight[]` | Dashboard, Body Weight |
| 6 | POST | [`/api/bodyweights`](#post-apibodyweights) | token | 201 `BodyWeight` | Body Weight |
| 7 | DELETE | [`/api/bodyweights/{id}`](#delete-apibodyweightsid) | token | 204 | Body Weight |
| 8 | GET | [`/api/exercises`](#get-apiexercises) | token | 200 `Exercise[]` | Exercises, Workout detail |
| 9 | GET | [`/api/exercises/popular`](#get-apiexercisespopular) | token | 200 `PopularExercise[]` | Dashboard |
| 10 | GET | [`/api/exercises/{id}`](#get-apiexercisesid) | token | 200 `Exercise` | Exercise detail |
| 11 | POST | [`/api/exercises`](#post-apiexercises) | token | 201 `Exercise` | Exercises |
| 12 | PUT | [`/api/exercises/{id}`](#put-apiexercisesid) | token | 200 `Exercise` | Exercise detail |
| 13 | DELETE | [`/api/exercises/{id}`](#delete-apiexercisesid) | token | 204 | Exercise detail |
| 14 | GET | [`/api/exercise-catalog`](#get-apiexercise-catalog) | token | 200 `CatalogExercise[]` | Exercises (add dialog) |
| 15 | GET | [`/api/exercises/{id}/logs`](#get-apiexercisesidlogs) | token | 200 `Log[]` | Exercise detail |
| 16 | GET | [`/api/exercises/{id}/logs/previous`](#get-apiexercisesidlogsprevious) | token | 200 `Log[]` | Live workout |
| 17 | POST | [`/api/exercises/{id}/logs`](#post-apiexercisesidlogs) | token | 201 `Log` | Exercise detail |
| 18 | DELETE | [`/api/logs/{id}`](#delete-apilogsid) | token | 204 | Exercise detail |
| 19 | GET | [`/api/workouts`](#get-apiworkouts) | token | 200 `Workout[]` | Workouts, Start Workout |
| 20 | GET | [`/api/workouts/{id}`](#get-apiworkoutsid) | token | 200 `Workout` | Workout detail, Live workout |
| 21 | POST | [`/api/workouts`](#post-apiworkouts) | token | 201 `Workout` | Workouts |
| 22 | PUT | [`/api/workouts/{id}`](#put-apiworkoutsid) | token | 200 `Workout` | Workout detail |
| 23 | DELETE | [`/api/workouts/{id}`](#delete-apiworkoutsid) | token | 204 | Workout detail |
| 24 | POST | [`/api/workouts/{id}/exercises`](#post-apiworkoutsidexercises) | token | 201 / 200 `WorkoutExercise` | Workout detail |
| 25 | DELETE | [`/api/workouts/{id}/exercises/{workoutExerciseId}`](#delete-apiworkoutsidexercisesworkoutexerciseid) | token | 204 | Workout detail |
| 26 | POST | [`/api/workouts/{id}/sessions`](#post-apiworkoutsidsessions) | token | 201 `WorkoutSessionResult` | Live workout |
| 27 | GET | [`/api/programs`](#get-apiprograms) | token | 200 `Program[]` | Programs |
| 28 | POST | [`/api/programs/{id}/copy`](#post-apiprogramsidcopy) | token | 201 `Workout` | Programs |
| 29 | GET | [`/api/nutrition/entries`](#get-apinutritionentries) | token | 200 `NutritionEntry[]` | Nutrition |
| 30 | POST | [`/api/nutrition/entries`](#post-apinutritionentries) | token | 201 `NutritionEntry` | Nutrition |
| 31 | DELETE | [`/api/nutrition/entries/{id}`](#delete-apinutritionentriesid) | token | 204 | Nutrition |
| 32 | PUT | [`/api/nutrition/goal`](#put-apinutritiongoal) | token | 200 `NutritionGoal` | Nutrition |
| 33 | GET | [`/api/nutrition/summary`](#get-apinutritionsummary) | token | 200 `NutritionSummary` | Nutrition |
| 34 | GET | [`/api/analytics/weekly`](#get-apianalyticsweekly) | token | 200 `WeeklyStat[]` | Dashboard |
| 35 | GET | [`/api/analytics/activity`](#get-apianalyticsactivity) | token | 200 `ActivityDay[]` | Dashboard |
| 36 | GET | [`/api/analytics/records`](#get-apianalyticsrecords) | token | 200 `PersonalRecord[]` | Records |
| 37 | GET | [`/api/export/csv`](#get-apiexportcsv) | token | 200 `text/csv` | Records |
| 38 | GET | [`/api/export/pdf`](#get-apiexportpdf) | token | 200 `application/pdf` | Records |

---

## Data types

These match the interfaces in `frontend/src/api/types.ts`. Java `record`s with the same field names work well
as DTOs.

```ts
User                 { id, email, name: string|null, image: string|null, xp: int }
AuthResponse         { token: string, user: User }
UserStats            { xp: int, level: int, current: int, needed: int, percent: int }

BodyWeight           { id, weight: number, createdAt: Instant }

Exercise             { id, name, description, muscleGroup: string|null, equipment: string|null, createdAt: Instant }
PopularExercise      { id, name, logCount: int }
CatalogExercise      { id, name, muscleGroup, equipment, instructions }
Log                  { id, exerciseId, weight: number, reps: int, oneRepMax: int, createdAt: Instant }

WorkoutExercise      { id /* link id */, exerciseId, name }
Workout              { id, name, description, createdAt: Instant, exercises: WorkoutExercise[] }
Program              = Workout
WorkoutSessionResult { logsCreated: int, xpAwarded: int }

Macros               { calories: int, protein: number, carbs: number, fat: number }
NutritionEntry       = Macros + { id, createdAt: Instant }
NutritionGoal        = Macros
NutritionSummary     { goal: NutritionGoal|null, consumed: Macros }

WeeklyStat           { week: LocalDate, volume: number, sessions: int }
ActivityDay          { date: LocalDate, count: int }
PersonalRecord       { exerciseId, exerciseName, bestWeight: number, bestOneRepMax: int, bestVolume: number, achievedAt: LocalDate }
```

---

## Auth

### `POST /api/auth/register`

Creates an account and signs the user in.

- **Used by:** Register page (`/register`), `auth/AuthContext.tsx`
- **Auth:** public

**Request**

```json
{ "email": "lifter@example.com", "password": "correct-horse" }
```

| Field | Type | Rules |
|---|---|---|
| `email` | string | required, valid email. Store it trimmed and lower-cased. |
| `password` | string | required, at least 8 characters |

The "confirm password" field is checked in the browser and isn't sent.

**Response `201 Created`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9…",
  "user": { "id": "3f1c…", "email": "lifter@example.com", "name": null, "image": null, "xp": 0 }
}
```

**Errors**

| Status | `detail` |
|---|---|
| 400 | `Please enter an email` / `Not a valid email` / `Password must be at least 8 characters` |
| 409 | `An account with this email already exists` |

Hash the password with the `PasswordEncoder` bean (BCrypt). Never return it.

---

### `POST /api/auth/login`

Checks the user's email and password and returns a token.

- **Used by:** Login page (`/login`), `auth/AuthContext.tsx`
- **Auth:** public

**Request**

```json
{ "email": "lifter@example.com", "password": "correct-horse" }
```

**Response `200 OK`**: same shape as register (`AuthResponse`).

**Errors**

| Status | `detail` |
|---|---|
| 400 | `Please enter an email` / `Please enter a password` |
| 401 | `Email or password is incorrect`. Use the same message whether the email or the password is wrong. |

---

## Users

### `GET /api/users/me`

Returns the signed-in user. The frontend calls it on page load when it already has a token, to confirm the
token still works and to show the email and avatar.

- **Used by:** Sidebar on every `/dashboard/*` page, `auth/AuthContext.tsx`
- **Auth:** token

**Response `200 OK`**: `User`

```json
{ "id": "3f1c…", "email": "lifter@example.com", "name": null, "image": null, "xp": 140 }
```

**Errors:** 401 if the token is missing or invalid. The frontend then logs the user out.

---

### `GET /api/users/me/stats`

Returns the user's level and progress toward the next level, calculated from their total XP.

- **Used by:** Level badge in the sidebar (`components/LevelBadge.tsx`)
- **Auth:** token

**Response `200 OK`**: `UserStats`

```json
{ "xp": 150, "level": 2, "current": 50, "needed": 200, "percent": 25 }
```

See [XP and levels](#xp-and-levels) for the formulas.

---

## Body weight

### `GET /api/bodyweights`

Lists the user's body weight entries.

- **Used by:** Dashboard (bodyweight chart), Body Weight page (`/dashboard/bodyweights`: chart, stats, table)
- **Auth:** token
- **Sort:** oldest first

**Response `200 OK`**: `BodyWeight[]`

```json
[
  { "id": "a1…", "weight": 84.0, "createdAt": "2026-05-01T07:30:00Z" },
  { "id": "b2…", "weight": 82.5, "createdAt": "2026-06-11T07:30:00Z" }
]
```

An empty list `[]` if there are no entries. The frontend computes highest, lowest, average, and the 7-, 30- and
365-day change itself.

---

### `POST /api/bodyweights`

Adds a body weight entry with the current time.

- **Used by:** Body Weight page, "Add weight" dialog
- **Auth:** token

**Request**

```json
{ "weight": 82.5 }
```

| Field | Type | Rules |
|---|---|---|
| `weight` | number | required, > 0 |

**Response `201 Created`**: the new `BodyWeight`.

**Errors:** 400 `Please enter a weight`.

---

### `DELETE /api/bodyweights/{id}`

Deletes one body weight entry.

- **Used by:** Body Weight page, the trash icon in the table
- **Auth:** token

| Path param | Description |
|---|---|
| `id` | Body weight entry id |

**Response `204 No Content`**

**Errors:** 404 if it doesn't exist or isn't the user's.

---

## Exercises

### `GET /api/exercises`

Lists the user's own exercises.

- **Used by:** Exercises page (`/dashboard/exercises`, card grid); Workout detail ("Add exercise" dropdown)
- **Auth:** token
- **Sort:** newest first

**Response `200 OK`**: `Exercise[]`

```json
[
  {
    "id": "e1…",
    "name": "Barbell Bench Press",
    "description": "Lie on a flat bench, lower the bar to mid-chest, press up to lockout.",
    "muscleGroup": "Chest",
    "equipment": "Barbell",
    "createdAt": "2026-06-01T10:00:00Z"
  }
]
```

---

### `GET /api/exercises/popular`

Returns the user's most-logged exercises.

- **Used by:** Dashboard, "Popular Exercises" card
- **Auth:** token

| Query param | Type | Default | Description |
|---|---|---|---|
| `limit` | int | 5 | Maximum number of exercises to return. The frontend sends `5`. |

**Sort:** by number of logs, highest first. Exercises with 0 logs may be included at the end.

**Response `200 OK`**: `PopularExercise[]`

```json
[
  { "id": "e1…", "name": "Back Squat", "logCount": 42 },
  { "id": "e2…", "name": "Barbell Bench Press", "logCount": 37 }
]
```

---

### `GET /api/exercises/{id}`

Returns one exercise.

- **Used by:** Exercise detail page (`/dashboard/exercises/:id`, title and description)
- **Auth:** token

**Response `200 OK`**: `Exercise`

**Errors:** 404 if it doesn't exist or isn't the user's. The page shows the error.

---

### `POST /api/exercises`

Creates an exercise. When the user picks an entry from the library, the frontend fills in `muscleGroup` and
`equipment` from it.

- **Used by:** Exercises page, "Add exercise" dialog
- **Auth:** token

**Request**

```json
{
  "name": "Barbell Bench Press",
  "description": "Lie on a flat bench, lower the bar to mid-chest, press up to lockout.",
  "muscleGroup": "Chest",
  "equipment": "Barbell"
}
```

| Field | Type | Rules |
|---|---|---|
| `name` | string | required, 1–40 characters after trimming |
| `description` | string | required, not blank |
| `muscleGroup` | string \| null | optional |
| `equipment` | string \| null | optional |

**Response `201 Created`**: the new `Exercise`.

**Errors:** 400 `Enter a name and a description` / `Name must be at most 40 characters`.

---

### `PUT /api/exercises/{id}`

Updates an exercise's name and description. The frontend always sends both fields.

- **Used by:** Exercise detail page, the "Edit name" and "Edit description" pencil icons
- **Auth:** token

**Request**

```json
{ "name": "Paused Bench", "description": "2-second pause on the chest." }
```

Validation is the same as `POST /api/exercises` for `name` and `description`. `muscleGroup` and
`equipment` don't change.

**Response `200 OK`**: the updated `Exercise`.

**Errors:** 400 (validation), 404.

---

### `DELETE /api/exercises/{id}`

Deletes an exercise, **all of its logs**, and its place in any workouts. The database `ON DELETE CASCADE`
rules handle this.

- **Used by:** Exercise detail page, Delete button
- **Auth:** token

**Response `204 No Content`**. The frontend then goes to `/dashboard/exercises`.

**Errors:** 404.

---

## Exercise catalog

### `GET /api/exercise-catalog`

Returns the global, read-only exercise library seeded by Flyway `V2` (34 exercises). It's the same for every
user.

- **Used by:** Exercises page, the "Pick from library" search box in the Add exercise dialog (grouped by
  muscle group)
- **Auth:** token
- **Sort:** `muscleGroup`, then `name`

**Response `200 OK`**: `CatalogExercise[]`

```json
[
  { "id": "c1…", "name": "Deadlift", "muscleGroup": "Back", "equipment": "Barbell", "instructions": "Hinge at the hips with a flat back, drive through the floor to stand tall." }
]
```

The frontend caches this for the whole session.

---

## Logs

A log is one set of an exercise: weight × reps, with an estimated 1RM calculated by the server.

### `GET /api/exercises/{id}/logs`

Returns all logs for one exercise.

- **Used by:** Exercise detail page (1RM progress chart and log table)
- **Auth:** token
- **Sort:** oldest first. The chart reads left to right; the table reverses the list.

**Response `200 OK`**: `Log[]`

```json
[
  { "id": "l1…", "exerciseId": "e1…", "weight": 100, "reps": 5, "oneRepMax": 112, "createdAt": "2026-06-11T18:02:00Z" }
]
```

**Errors:** 404 if the exercise isn't the user's.

---

### `GET /api/exercises/{id}/logs/previous`

Returns the user's **most recent** sets for an exercise. The live workout tracker shows them in a
"Previous" column next to each set.

- **Used by:** Live workout page (`/dashboard/runningworkout/:id`)
- **Auth:** token

| Query param | Type | Description |
|---|---|---|
| `limit` | int, required | How many sets to return. The frontend sends the number of sets currently on screen (1, 2, 3…). |

**Sort:** **newest first**. The frontend reverses the list so set 1 lines up with set 1.

**Response `200 OK`**: `Log[]` (at most `limit` items; `[]` if there are none).

**Errors:** 404 if the exercise isn't the user's.

---

### `POST /api/exercises/{id}/logs`

Logs one set. The server calculates `oneRepMax` and gives the user `XP_PER_SET` (10) XP.

- **Used by:** Exercise detail page, "Add new log" dialog
- **Auth:** token

**Request**

```json
{ "weight": 100, "reps": 5 }
```

| Field | Type | Rules |
|---|---|---|
| `weight` | number | required, >= 0 (0 for bodyweight exercises) |
| `reps` | int | required, > 0 |

**Response `201 Created`**: the new `Log`, including `oneRepMax`.

**Errors:** 400 `Enter a weight and the reps`, 404 if the exercise isn't the user's.

---

### `DELETE /api/logs/{id}`

Deletes one log. XP that was already awarded isn't taken back.

- **Used by:** Exercise detail page, the trash icon in the log table
- **Auth:** token

**Response `204 No Content`**

**Errors:** 404.

---

## Workouts

A workout is a named list of the user's exercises. In `Workout.exercises`, each item's `id` is the
**link id** (a `workout_exercises` row), which is used to remove it. `exerciseId` is the exercise itself.

### `GET /api/workouts`

Lists the user's workouts, each with its exercises. Program templates are not included.

- **Used by:** Workouts page (`/dashboard/workouts`), Start Workout page (`/dashboard/runningworkout`)
- **Auth:** token
- **Sort:** newest first. Each workout's `exercises` are in the order they were added.

**Response `200 OK`**: `Workout[]`

```json
[
  {
    "id": "w1…",
    "name": "Push Day",
    "description": "Chest, shoulders, triceps",
    "createdAt": "2026-06-01T10:00:00Z",
    "exercises": [
      { "id": "link1…", "exerciseId": "e1…", "name": "Barbell Bench Press" },
      { "id": "link2…", "exerciseId": "e5…", "name": "Overhead Press" }
    ]
  }
]
```

Fetch the exercises in bulk (join fetch or entity graph) so a long list doesn't trigger one query per workout.

---

### `GET /api/workouts/{id}`

Returns one workout with its exercises.

- **Used by:** Workout detail page (`/dashboard/workouts/:id`), Live workout page (`/dashboard/runningworkout/:id`)
- **Auth:** token

**Response `200 OK`**: `Workout`

**Errors:** 404 if it doesn't exist, isn't the user's, or is a template.

---

### `POST /api/workouts`

Creates an empty workout.

- **Used by:** Workouts page, "Add workout" dialog
- **Auth:** token

**Request**

```json
{ "name": "Push Day", "description": "Chest, shoulders, triceps" }
```

| Field | Type | Rules |
|---|---|---|
| `name` | string | required, 1–40 characters after trimming |
| `description` | string | required, not blank |

**Response `201 Created`**: the new `Workout`, with `"exercises": []`.

**Errors:** 400 `Enter a name and a description` / `Name must be at most 40 characters`.

---

### `PUT /api/workouts/{id}`

Updates a workout's name and description. The frontend always sends both.

- **Used by:** Workout detail page, pencil icons
- **Auth:** token

**Request:** same as `POST /api/workouts`.

**Response `200 OK`**: the updated `Workout`.

**Errors:** 400, 404.

---

### `DELETE /api/workouts/{id}`

Deletes a workout and its exercise links. The exercises and their logs are kept.

- **Used by:** Workout detail page, Delete button
- **Auth:** token

**Response `204 No Content`**. The frontend then goes to `/dashboard/workouts`.

**Errors:** 404.

---

### `POST /api/workouts/{id}/exercises`

Adds one of the user's exercises to a workout. Adding the same exercise twice is safe: it doesn't create a
duplicate.

- **Used by:** Workout detail page, "Add exercise" dialog
- **Auth:** token

**Request**

```json
{ "exerciseId": "e1…" }
```

**Response**

- `201 Created`: the new `WorkoutExercise`
- `200 OK`: the existing `WorkoutExercise`, if the exercise is already in the workout

```json
{ "id": "link1…", "exerciseId": "e1…", "name": "Barbell Bench Press" }
```

**Errors:** 400 if `exerciseId` is missing, 404 if the workout or the exercise isn't the user's.

---

### `DELETE /api/workouts/{id}/exercises/{workoutExerciseId}`

Removes an exercise from a workout. The exercise itself is not deleted.

- **Used by:** Workout detail page, the trash icon on each exercise row
- **Auth:** token

| Path param | Description |
|---|---|
| `id` | Workout id |
| `workoutExerciseId` | The **link** id (`WorkoutExercise.id`), not the exercise id |

**Response `204 No Content`**

**Errors:** 404 if the link isn't part of this workout, or the workout isn't the user's.

---

### `POST /api/workouts/{id}/sessions`

Finishes a live workout. Saves every set the user entered **in one transaction** and awards XP.

- **Used by:** Live workout page, "Finish workout" button
- **Auth:** token

**Request**

```json
{
  "exercises": [
    { "exerciseId": "e1…", "sets": [{ "weight": 100, "reps": 5 }, { "weight": 100, "reps": 5 }] },
    { "exerciseId": "e5…", "sets": [{ "weight": 60, "reps": 8 }] }
  ]
}
```

| Field | Rules |
|---|---|
| `exercises` | required, at least 1 item |
| `exercises[].exerciseId` | must be the user's exercise. The frontend only sends exercises from this workout. |
| `exercises[].sets` | at least 1 item. Each set follows the same rules as `POST /api/exercises/{id}/logs`. |

The frontend leaves out empty or incomplete sets before sending.

**Behaviour**

- Each set becomes a `Log` with `oneRepMax` calculated, all with the same timestamp.
- `xpAwarded = logsCreated × XP_PER_SET + XP_PER_WORKOUT`. In the example, 3 × 10 + 25 = 55.
- All or nothing: if anything is invalid, nothing is saved.

**Response `201 Created`**: `WorkoutSessionResult`

```json
{ "logsCreated": 3, "xpAwarded": 55 }
```

The frontend shows these numbers in a "Workout complete" dialog.

**Errors:** 400 (empty session or an invalid set), 404 (the workout or an exercise isn't the user's).

---

## Programs

Programs are public templates (PPL, StrongLifts 5×5, Starting Strength) seeded by Flyway `V2`. They're
`workouts` rows with `is_template = true AND is_public = true`, owned by the system user
`system@lockedin.internal`.

### `GET /api/programs`

Lists all public program templates.

- **Used by:** Programs page (`/dashboard/programs`)
- **Auth:** token
- **Sort:** by name

**Response `200 OK`**: `Program[]` (same shape as `Workout`)

```json
[
  {
    "id": "p1…",
    "name": "PPL Push",
    "description": "Push day: chest, shoulders, triceps.",
    "createdAt": "2026-01-01T00:00:00Z",
    "exercises": [
      { "id": "link…", "exerciseId": "sys-e1…", "name": "Barbell Bench Press" }
    ]
  }
]
```

---

### `POST /api/programs/{id}/copy`

Copies a program into the user's own workouts.

- **Used by:** Programs page, "Use this program" button. The frontend then opens the new workout.
- **Auth:** token
- **Request body:** none

**Behaviour**

1. Create a workout for the user with the program's name and description.
2. For each exercise in the program, use the user's exercise **with the same name** if they have one.
   Otherwise create a copy for the user (same name, description, muscle group and equipment).
3. Link those exercises to the new workout in the same order.

**Response `201 Created`**: the new `Workout`.

**Errors:** 404 if `id` isn't a public template.

---

## Nutrition

### `GET /api/nutrition/entries`

Lists the user's nutrition entries.

- **Used by:** Nutrition page (`/dashboard/nutrition`: calorie chart and table)
- **Auth:** token
- **Sort:** oldest first

**Response `200 OK`**: `NutritionEntry[]`

```json
[
  { "id": "n1…", "calories": 650, "protein": 40, "carbs": 70, "fat": 20, "createdAt": "2026-06-11T12:30:00Z" }
]
```

---

### `POST /api/nutrition/entries`

Logs a meal or snack with the current time.

- **Used by:** Nutrition page, "Add entry" dialog. When the user enables auto mode, the frontend calculates
  calories from the macros.
- **Auth:** token

**Request**

```json
{ "calories": 650, "protein": 40, "carbs": 70, "fat": 20 }
```

| Field | Type | Rules |
|---|---|---|
| `calories` | int | required, >= 0 |
| `protein`, `carbs`, `fat` | number | >= 0. The frontend always sends them (0 if left empty). |

**Response `201 Created`**: the new `NutritionEntry`.

**Errors:** 400 `Enter a calorie amount` / `Calories cannot be negative` / `Macros cannot be negative`.

---

### `DELETE /api/nutrition/entries/{id}`

Deletes one nutrition entry.

- **Used by:** Nutrition page, the trash icon in the table
- **Auth:** token

**Response `204 No Content`**

**Errors:** 404.

---

### `PUT /api/nutrition/goal`

Creates or replaces the user's daily target (one per user).

- **Used by:** Nutrition page, the "Set goal" / "Edit goal" dialog
- **Auth:** token

**Request:** `Macros`. Validation is the same as `POST /api/nutrition/entries`.

```json
{ "calories": 2500, "protein": 180, "carbs": 250, "fat": 80 }
```

**Response `200 OK`**: the saved `NutritionGoal` (same shape).

**Errors:** 400.

---

### `GET /api/nutrition/summary`

Returns the user's daily goal and how much they've eaten so far today.

- **Used by:** Nutrition page, the "Today" card (calories remaining and macro progress bars)
- **Auth:** token

| Query param | Type | Description |
|---|---|---|
| `since` | Instant, optional | The start of the user's **local** day, e.g. `2026-06-11T22:00:00Z`. The browser sends this so "today" follows the user's timezone. If it's missing, use the start of the current UTC day. |

**Response `200 OK`**: `NutritionSummary`

```json
{
  "goal": { "calories": 2500, "protein": 180, "carbs": 250, "fat": 80 },
  "consumed": { "calories": 1200, "protein": 90, "carbs": 110, "fat": 40 }
}
```

- `goal` is `null` if the user hasn't set one. The page then shows a "Set goal" prompt.
- `consumed` adds up the entries with `createdAt >= since`. It's all zeros if there are none.

---

## Analytics

All three endpoints are calculated from the user's logs. The exact rules and test cases are under
[Business rules](#business-rules).

### `GET /api/analytics/weekly`

Returns total training volume and number of training days for each week.

- **Used by:** Dashboard, "Weekly Volume" chart
- **Auth:** token
- **Sort:** oldest week first. Only weeks that have logs.

**Response `200 OK`**: `WeeklyStat[]`

```json
[
  { "week": "2026-06-08", "volume": 1400, "sessions": 2 },
  { "week": "2026-06-15", "volume": 300, "sessions": 1 }
]
```

---

### `GET /api/analytics/activity`

Returns the number of sets logged on each day, for the activity heatmap.

- **Used by:** Dashboard, "Activity" heatmap (rolling 12 months, plus a picker for earlier years)
- **Auth:** token
- **Sort:** any. Include only days with at least one set, across all years. The year buttons come from
  these dates.

**Response `200 OK`**: `ActivityDay[]`

```json
[
  { "date": "2026-06-08", "count": 2 },
  { "date": "2026-06-09", "count": 1 }
]
```

---

### `GET /api/analytics/records`

Returns the user's best results for each exercise they've logged.

- **Used by:** Records page (`/dashboard/records`, table)
- **Auth:** token
- **Sort:** by `exerciseName`

**Response `200 OK`**: `PersonalRecord[]`

```json
[
  { "exerciseId": "e1…", "exerciseName": "Bench", "bestWeight": 100, "bestOneRepMax": 109, "bestVolume": 400, "achievedAt": "2026-06-08" }
]
```

---

## Export

The frontend downloads exports with `fetch`, so the bearer token is sent, and saves the response as a file.
It uses the filename from `Content-Disposition`; `SecurityConfig` already lets the browser read that
header.

### `GET /api/export/csv`

Downloads one of the user's data sets as CSV.

- **Used by:** Records page, the "Logs CSV" / "Bodyweight CSV" / "Nutrition CSV" buttons
- **Auth:** token

| Query param | Values | Default |
|---|---|---|
| `type` | `logs`, `bodyweight`, `nutrition` | `logs` |

**Response `200 OK`**

```
Content-Type: text/csv; charset=utf-8
Content-Disposition: attachment; filename="lockedin-logs.csv"
```

Rows are oldest first; `date` is `YYYY-MM-DD` (UTC).

| `type` | Header row | Example row |
|---|---|---|
| `logs` | `date,exercise,weight_kg,reps,est_1rm_kg` | `2026-06-11,Barbell Bench Press,100,5,112` |
| `bodyweight` | `date,weight_kg` | `2026-06-11,82.5` |
| `nutrition` | `date,calories,protein_g,carbs_g,fat_g` | `2026-06-11,650,40,70,20` |

Escape fields per RFC 4180: if a value contains `,`, `"`, CR or LF, wrap it in `"` and double any `"` inside
(`Curl, "21s"` → `"Curl, ""21s"""`).

**Errors:** 400 for an unknown `type`.

---

### `GET /api/export/pdf`

Downloads a one-page progress report as a PDF.

- **Used by:** Records page, "Progress report (PDF)" button
- **Auth:** token

**Response `200 OK`**

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="lockedin-progress-report.pdf"
```

Contents: the user's email, the date it was generated, total sets, total volume (Σ weight × reps), first and
latest body weight (or "—"), and the personal records table from `/api/analytics/records`. OpenPDF and
Apache PDFBox both work for this.

---

## Business rules

These come from the original app and its test suite. Each example can be used directly as a test case.

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

For the same weight, more reps gives a higher estimate. The result is always a whole number.

Reps are capped at 36: at 37 the denominator turns negative and the estimate stops making sense. Validation
rejects anything above 36 with a 400.

### XP and levels

- `XP_PER_SET = 10` for every logged set (a single log or a set in a session).
- `XP_PER_WORKOUT = 25` bonus for each finished session.
- XP needed to *reach* level n: `xpForLevel(n) = 50 × (n − 1) × n` (level 1 = 0, 2 = 100, 3 = 300, 4 = 600).
- `level` is the highest n where `xpForLevel(n) <= xp`. Negative XP counts as level 1.
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
- Week key: the Monday (UTC) that starts the week. Thursday `2026-06-11` → `2026-06-08`;
  Sunday `2026-06-14` → `2026-06-08`; Monday `2026-06-08T23:59Z` → `2026-06-08`.

### Weekly stats

Group logs by week key. `volume` = Σ weight × reps. `sessions` = the number of different day keys in that
week. Sort oldest first. No logs → `[]`.

| createdAt | weight × reps |
|---|---|
| 2026-06-08T10:00Z (Mon) | 100 × 5 |
| 2026-06-08T11:00Z (Mon) | 50 × 10 |
| 2026-06-10T10:00Z (Wed) | 80 × 5 |
| 2026-06-15T10:00Z (next Mon) | 60 × 5 |

→ `[{ week: "2026-06-08", volume: 1400, sessions: 2 }, { week: "2026-06-15", volume: 300, sessions: 1 }]`

### Activity

Count logs per day key. Two logs on `2026-06-08` and one on `2026-06-09` →
`[{ date: "2026-06-08", count: 2 }, { date: "2026-06-09", count: 1 }]`.

### Personal records

For each exercise:
- `bestWeight` = the highest weight
- `bestOneRepMax` = the highest oneRepMax. `achievedAt` = the day key of that set. If two sets tie, the first
  one counts.
- `bestVolume` = the highest weight × reps from a single set

Sort by exercise name. Example: Bench logs `80×5 (1RM 90, 2026-06-01)` and `100×3 (1RM 109, 2026-06-08)`
→ `bestWeight 100, bestOneRepMax 109, bestVolume 400` (80×5 beats 100×3), `achievedAt 2026-06-08`.

---

## Validation rules

The frontend checks these before sending a request, but the backend must check them as well. If the server
rejects a request, the UI shows the message from the table.

| Field | Rule | Message |
|---|---|---|
| email | required, valid email | `Please enter an email` / `Not a valid email` |
| password (login) | required | `Please enter a password` |
| password (register) | at least 8 characters | `Password must be at least 8 characters` |
| exercise / workout `name` | required, 1–40 characters after trimming | `Enter a name and a description` / `Name must be at most 40 characters` |
| exercise / workout `description` | required, not blank | `Enter a name and a description` |
| body weight `weight` | > 0 | `Please enter a weight` |
| log / set `weight` | >= 0 | `Enter a weight and the reps` |
| log / set `reps` | whole number, 1 to 36 | `Enter a weight and the reps` / `At most 36 reps per set` |
| nutrition `calories` | required whole number >= 0 | `Enter a calorie amount` / `Calories cannot be negative` |
| nutrition `protein` / `carbs` / `fat` | >= 0, default 0 | `Macros cannot be negative` |

Changes from the original Next.js app:
- Names can be up to 40 characters (was 15, which cut off library names like "Incline Dumbbell Press").
- The minimum password length is 8 (was 5).
- A set's weight can be 0, for bodyweight exercises.

---

## Endpoints by screen

Use this to decide what to build first. A screen works fully once all of its endpoints are done.

| Screen | Route | Endpoints |
|---|---|---|
| Register | `/register` | 1 |
| Login | `/login` | 2 |
| Sidebar (every dashboard page) | `/dashboard/*` | 3, 4 |
| Dashboard | `/dashboard` | 5, 9, 34, 35 |
| Body Weight | `/dashboard/bodyweights` | 5, 6, 7 |
| Exercises | `/dashboard/exercises` | 8, 11, 14 |
| Exercise detail | `/dashboard/exercises/:id` | 10, 12, 13, 15, 17, 18 |
| Workouts | `/dashboard/workouts` | 19, 21 |
| Workout detail | `/dashboard/workouts/:id` | 8, 20, 22, 23, 24, 25 |
| Start Workout | `/dashboard/runningworkout` | 19 |
| Live workout | `/dashboard/runningworkout/:id` | 16, 20, 26 |
| Programs | `/dashboard/programs` | 27, 28 |
| Records | `/dashboard/records` | 36, 37, 38 |
| Nutrition | `/dashboard/nutrition` | 29, 30, 31, 32, 33 |
