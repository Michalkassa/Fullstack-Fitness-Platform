<div align="center">

<img src="frontend/public/logo.png" alt="LockedIn logo" width="84" />

# LockedIn

**A full-stack fitness tracker for logging workouts, tracking strength, and staying on top of your goals.**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-7-3178C6?logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3-06B6D4?logo=tailwindcss&logoColor=white)

</div>

<p align="center">
  <img src="docs/screenshots/landing.svg" alt="LockedIn landing page" width="900" />
</p>

LockedIn is split into two apps:

- **`backend/`**: a **Spring Boot** REST API (Java 25, Spring Data JPA, Spring Security with JWT, Flyway,
  PostgreSQL), built test-first.
- **`frontend/`**: a **React + TypeScript** single-page app (Vite, React Router, TanStack Query,
  Tailwind CSS, Chart.js) that talks to the API.

The contract between them is documented in **[docs/API.md](docs/API.md)**.

> **Status:** this branch is a rewrite of the original Next.js app. The frontend is complete. The backend
> has the project skeleton, database schema and seed data; the endpoints are being implemented with
> TDD. See [backend/README.md](backend/README.md) for the roadmap.

> The previews below are stylised SVG mockups of the interface.

---

## Features

### Dashboard
Everything at a glance: quick actions, your bodyweight trend, most-trained exercises, weekly volume, and a
GitHub-style activity heatmap that rolls to today.

<p align="center"><img src="docs/screenshots/dashboard.svg" alt="Dashboard" width="900" /></p>

### Workouts and a live set tracker
Build workouts from your exercises and run a live session with a timer. It records each set, shows your
previous numbers, keeps your progress if you refresh, and awards XP when you finish.

<p align="center"><img src="docs/screenshots/live-workout.svg" alt="Live workout tracker" width="900" /></p>

### Exercises and automatic 1RM
Log weight × reps and LockedIn estimates your one-rep max (Brzycki formula) and charts it over time. Pick
from a built-in library of 30+ exercises tagged by muscle group, or add your own.

<p align="center"><img src="docs/screenshots/exercise.svg" alt="Exercise detail with 1RM chart" width="900" /></p>

### Prebuilt programs
Start from proven programs (Push/Pull/Legs, StrongLifts 5×5, Starting Strength) and copy them into your
account in one click.

<p align="center"><img src="docs/screenshots/programs.svg" alt="Programs" width="900" /></p>

### Personal records and data export
Track your best weight, estimated 1RM and set volume per exercise, and export everything to CSV or a
generated PDF progress report.

<p align="center"><img src="docs/screenshots/records.svg" alt="Personal records" width="900" /></p>

### Nutrition with daily goals
Set a daily calorie and macro goal, log meals (enter calories or calculate them from macros), and see
what's left for the day.

<p align="center"><img src="docs/screenshots/nutrition.svg" alt="Nutrition tracking" width="900" /></p>

### Activity heatmap and gamification
A trailing-year contribution graph (with a year selector for history) plus an XP/level system that
rewards consistency.

<p align="center"><img src="docs/screenshots/heatmap.svg" alt="Activity heatmap" width="820" /></p>

---

## Tech stack

| Layer | Tech |
|------|------|
| Backend | Spring Boot 4 · Java 25 · Spring Web MVC · Spring Data JPA · Spring Security (JWT) · Bean Validation |
| Database | PostgreSQL 16 · Flyway migrations |
| Backend tests | JUnit 5 · MockMvc · Mockito · Testcontainers |
| Frontend | React 19 · TypeScript · Vite · React Router · TanStack Query |
| UI | Tailwind CSS · Headless UI · React Icons · react-select |
| Charts | Chart.js · custom contribution heatmap |
| Frontend tests | Vitest · Testing Library |
| Tooling | Docker · docker-compose · nginx |

---

## Project structure

```
.
├── backend/                 Spring Boot API (Maven)
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/    Flyway: V1 schema, V2 exercise catalog + programs
├── frontend/                React + TypeScript SPA (Vite)
│   └── src/
│       ├── api/             types.ts (API shapes), endpoints.ts, queries.ts (React Query hooks)
│       ├── auth/            token storage, AuthContext, route guards
│       ├── components/      shared UI
│       ├── layouts/         dashboard sidebar / bottom nav
│       ├── lib/             pure helpers (+ tests)
│       └── pages/           one file per route
├── docs/
│   ├── API.md               REST contract, business rules, validation rules
│   └── screenshots/
└── docker-compose.yml       db + backend + frontend
```

---

## Getting started

### Prerequisites
- Java 25 (Maven is included via `./mvnw`)
- Node.js 20+
- Docker (for PostgreSQL and Testcontainers)

### Local development

```bash
# 1. Database
docker compose up db

# 2. Backend -> http://localhost:8080
cd backend
./mvnw spring-boot:run

# 3. Frontend -> http://localhost:5173
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`, so there's no CORS setup in development.
Flyway creates the tables and seeds the exercise catalog and programs the first time the backend starts.

### Everything in Docker

```bash
cp .env.example .env    # set JWT_SECRET
docker compose up --build
```

Then open **http://localhost:3000**. nginx serves the frontend and forwards `/api` to the backend.

If ports 3000, 8080 or 5432 are already taken, set `FRONTEND_PORT`, `BACKEND_PORT` or `DB_PORT` in `.env`.
`docker compose down` stops everything; add `-v` to also delete the database volume.

---

## Testing

```bash
cd backend  && ./mvnw test     # JUnit; Testcontainers tests are skipped if Docker isn't running
cd frontend && npm test        # Vitest
```

---

## Documentation

- [docs/API.md](docs/API.md): every endpoint, request/response shapes, errors, business rules and
  validation rules
- [backend/README.md](backend/README.md): backend setup, architecture and the TDD roadmap
- [frontend/README.md](frontend/README.md): frontend setup and structure
