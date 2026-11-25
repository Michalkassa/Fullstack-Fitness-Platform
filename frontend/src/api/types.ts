/**
 * Request/response shapes for the LockedIn REST API.
 * This file mirrors docs/API.md: keep the two in sync when the contract changes.
 *
 * Conventions:
 *  - ids are UUID strings
 *  - timestamps are ISO-8601 instants in UTC, e.g. "2026-06-11T13:45:00Z"
 *  - weights are kilograms
 */

export type Id = string;
export type IsoInstant = string;
/** Calendar date, "YYYY-MM-DD". */
export type IsoDate = string;

// ---------- Auth & users ----------

export interface User {
  id: Id;
  email: string;
  name: string | null;
  image: string | null;
  xp: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface UserStats {
  xp: number;
  level: number;
  /** XP earned into the current level. */
  current: number;
  /** XP span of the current level. */
  needed: number;
  /** 0-100 progress toward the next level. */
  percent: number;
}

// ---------- Body weight ----------

export interface BodyWeight {
  id: Id;
  weight: number;
  createdAt: IsoInstant;
}

export interface CreateBodyWeightRequest {
  weight: number;
}

// ---------- Exercises & logs ----------

export interface Exercise {
  id: Id;
  name: string;
  description: string;
  muscleGroup: string | null;
  equipment: string | null;
  createdAt: IsoInstant;
}

export interface CreateExerciseRequest {
  name: string;
  description: string;
  muscleGroup?: string | null;
  equipment?: string | null;
}

export interface UpdateExerciseRequest {
  name: string;
  description: string;
}

export interface PopularExercise {
  id: Id;
  name: string;
  logCount: number;
}

export interface CatalogExercise {
  id: Id;
  name: string;
  muscleGroup: string;
  equipment: string;
  instructions: string;
}

export interface Log {
  id: Id;
  exerciseId: Id;
  weight: number;
  reps: number;
  /** Estimated 1RM computed by the server (Brzycki, floored). */
  oneRepMax: number;
  createdAt: IsoInstant;
}

export interface SetInput {
  weight: number;
  reps: number;
}

// ---------- Workouts & programs ----------

/** An exercise attached to a workout. `id` is the link id, not the exercise id. */
export interface WorkoutExercise {
  id: Id;
  exerciseId: Id;
  name: string;
}

export interface Workout {
  id: Id;
  name: string;
  description: string;
  createdAt: IsoInstant;
  exercises: WorkoutExercise[];
}

export interface WorkoutRequest {
  name: string;
  description: string;
}

export interface AddWorkoutExerciseRequest {
  exerciseId: Id;
}

/** Public program template. Same shape as a workout. */
export type Program = Workout;

export interface WorkoutSessionRequest {
  exercises: { exerciseId: Id; sets: SetInput[] }[];
}

export interface WorkoutSessionResult {
  logsCreated: number;
  xpAwarded: number;
}

// ---------- Nutrition ----------

export interface Macros {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
}

export interface NutritionEntry extends Macros {
  id: Id;
  createdAt: IsoInstant;
}

export type NutritionGoal = Macros;

export interface NutritionSummary {
  goal: NutritionGoal | null;
  consumed: Macros;
}

// ---------- Analytics ----------

export interface WeeklyStat {
  /** Monday (UTC) that starts the week. */
  week: IsoDate;
  /** Sum of weight × reps for the week. */
  volume: number;
  /** Distinct training days in the week. */
  sessions: number;
}

export interface ActivityDay {
  date: IsoDate;
  /** Number of sets logged that day. */
  count: number;
}

export interface PersonalRecord {
  exerciseId: Id;
  exerciseName: string;
  bestWeight: number;
  bestOneRepMax: number;
  /** Best single-set weight × reps. */
  bestVolume: number;
  /** Date of the best 1RM set. */
  achievedAt: IsoDate;
}

export type CsvExportType = "logs" | "bodyweight" | "nutrition";
