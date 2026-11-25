# LockedIn frontend

React 19 + TypeScript single-page app built with Vite. It talks to the Spring Boot API described in
[../docs/API.md](../docs/API.md).

## Running

```bash
npm install
npm run dev        # http://localhost:5173
```

The dev server proxies every `/api/*` request to `http://localhost:8080` (change it with
`VITE_BACKEND_URL`). Until an endpoint exists on the backend, the screen that uses it shows an inline
error instead of crashing, so you can build the API one endpoint at a time.

| Script | Does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Type-check and build to `dist/` |
| `npm run preview` | Serve the production build |
| `npm run typecheck` | `tsc` only |
| `npm test` / `npm run test:watch` | Vitest |

| Env var | Purpose |
|---|---|
| `VITE_API_URL` | Base URL the browser calls. Empty = same origin (`/api/...`), which is what dev and Docker use. |
| `VITE_BACKEND_URL` | Where the dev proxy sends `/api` (dev server only). |

## Structure

```
src/
├── api/
│   ├── types.ts        request/response types (mirror of docs/API.md)
│   ├── client.ts       fetch wrapper: base URL, bearer token, ProblemDetail → ApiError, 401 handling
│   ├── endpoints.ts    one function per backend endpoint
│   └── queries.ts      TanStack Query hooks + cache invalidation
├── auth/               AuthContext (token in localStorage, current user), RequireAuth / GuestOnly guards
├── components/         Modal, ConfirmDialog, LineChart, QueryState, EditableHeader, …
├── layouts/            DashboardLayout (sidebar + mobile bottom nav), PublicNavbar
├── lib/                pure helpers with tests: dates, nutrition, bodyweight stats, validation, session drafts
└── pages/              one component per route
```

## Routes

| Path | Page |
|---|---|
| `/` | Landing |
| `/login`, `/register` | Auth (redirect to the dashboard if already signed in) |
| `/dashboard` | Quick actions, bodyweight chart, popular exercises, weekly volume, heatmap |
| `/dashboard/runningworkout` | Pick a workout to start |
| `/dashboard/runningworkout/:id` | Live tracker: timer, sets, previous numbers, finish → XP |
| `/dashboard/workouts`, `/dashboard/workouts/:id` | Workout list / detail |
| `/dashboard/programs` | Program templates |
| `/dashboard/exercises`, `/dashboard/exercises/:id` | Exercise list / detail with 1RM chart and logs |
| `/dashboard/records` | Personal records and CSV/PDF export |
| `/dashboard/nutrition` | Daily goal, entries, calorie chart |
| `/dashboard/bodyweights` | Body weight chart, stats and entries |

Everything under `/dashboard` requires a token; a 401 from the API logs the user out.
