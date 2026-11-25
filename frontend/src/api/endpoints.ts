/**
 * One function per backend endpoint. docs/API.md documents each of these.
 */
import { http, requestBlob } from "./client";
import type {
  ActivityDay,
  AuthResponse,
  BodyWeight,
  CatalogExercise,
  CreateBodyWeightRequest,
  CreateExerciseRequest,
  CsvExportType,
  Exercise,
  Id,
  Log,
  LoginRequest,
  Macros,
  NutritionEntry,
  NutritionGoal,
  NutritionSummary,
  PersonalRecord,
  PopularExercise,
  Program,
  RegisterRequest,
  SetInput,
  UpdateExerciseRequest,
  User,
  UserStats,
  WeeklyStat,
  Workout,
  WorkoutExercise,
  WorkoutRequest,
  WorkoutSessionRequest,
  WorkoutSessionResult,
} from "./types";

const q = (params: Record<string, string | number | undefined>) => {
  const s = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== undefined) s.set(k, String(v));
  const str = s.toString();
  return str ? `?${str}` : "";
};

export const authApi = {
  register: (body: RegisterRequest) => http.post<AuthResponse>("/api/auth/register", body),
  login: (body: LoginRequest) => http.post<AuthResponse>("/api/auth/login", body),
};

export const usersApi = {
  me: () => http.get<User>("/api/users/me"),
  stats: () => http.get<UserStats>("/api/users/me/stats"),
};

export const bodyWeightApi = {
  list: () => http.get<BodyWeight[]>("/api/bodyweights"),
  create: (body: CreateBodyWeightRequest) => http.post<BodyWeight>("/api/bodyweights", body),
  remove: (id: Id) => http.delete(`/api/bodyweights/${id}`),
};

export const exerciseApi = {
  list: () => http.get<Exercise[]>("/api/exercises"),
  popular: (limit = 5) => http.get<PopularExercise[]>(`/api/exercises/popular${q({ limit })}`),
  get: (id: Id) => http.get<Exercise>(`/api/exercises/${id}`),
  create: (body: CreateExerciseRequest) => http.post<Exercise>("/api/exercises", body),
  update: (id: Id, body: UpdateExerciseRequest) => http.put<Exercise>(`/api/exercises/${id}`, body),
  remove: (id: Id) => http.delete(`/api/exercises/${id}`),
  catalog: () => http.get<CatalogExercise[]>("/api/exercise-catalog"),
};

export const logApi = {
  listForExercise: (exerciseId: Id) => http.get<Log[]>(`/api/exercises/${exerciseId}/logs`),
  previous: (exerciseId: Id, limit: number) =>
    http.get<Log[]>(`/api/exercises/${exerciseId}/logs/previous${q({ limit })}`),
  create: (exerciseId: Id, body: SetInput) => http.post<Log>(`/api/exercises/${exerciseId}/logs`, body),
  remove: (logId: Id) => http.delete(`/api/logs/${logId}`),
};

export const workoutApi = {
  list: () => http.get<Workout[]>("/api/workouts"),
  get: (id: Id) => http.get<Workout>(`/api/workouts/${id}`),
  create: (body: WorkoutRequest) => http.post<Workout>("/api/workouts", body),
  update: (id: Id, body: WorkoutRequest) => http.put<Workout>(`/api/workouts/${id}`, body),
  remove: (id: Id) => http.delete(`/api/workouts/${id}`),
  addExercise: (workoutId: Id, exerciseId: Id) =>
    http.post<WorkoutExercise>(`/api/workouts/${workoutId}/exercises`, { exerciseId }),
  removeExercise: (workoutId: Id, workoutExerciseId: Id) =>
    http.delete(`/api/workouts/${workoutId}/exercises/${workoutExerciseId}`),
  completeSession: (workoutId: Id, body: WorkoutSessionRequest) =>
    http.post<WorkoutSessionResult>(`/api/workouts/${workoutId}/sessions`, body),
};

export const programApi = {
  list: () => http.get<Program[]>("/api/programs"),
  copy: (programId: Id) => http.post<Workout>(`/api/programs/${programId}/copy`),
};

export const nutritionApi = {
  listEntries: () => http.get<NutritionEntry[]>("/api/nutrition/entries"),
  createEntry: (body: Macros) => http.post<NutritionEntry>("/api/nutrition/entries", body),
  removeEntry: (id: Id) => http.delete(`/api/nutrition/entries/${id}`),
  setGoal: (body: Macros) => http.put<NutritionGoal>("/api/nutrition/goal", body),
  /** `since` = start of the user's local day, so "today" follows their timezone. */
  summary: (since: string) => http.get<NutritionSummary>(`/api/nutrition/summary${q({ since })}`),
};

export const analyticsApi = {
  weekly: () => http.get<WeeklyStat[]>("/api/analytics/weekly"),
  activity: () => http.get<ActivityDay[]>("/api/analytics/activity"),
  records: () => http.get<PersonalRecord[]>("/api/analytics/records"),
};

export const exportApi = {
  csv: (type: CsvExportType) => requestBlob(`/api/export/csv${q({ type })}`),
  pdf: () => requestBlob("/api/export/pdf"),
};
