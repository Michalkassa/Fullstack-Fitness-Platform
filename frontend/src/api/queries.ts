/**
 * React Query hooks wrapping the endpoints. Mutations invalidate whatever data
 * they change, so screens refresh without manual refetching.
 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  analyticsApi,
  bodyWeightApi,
  exerciseApi,
  logApi,
  nutritionApi,
  programApi,
  usersApi,
  workoutApi,
} from "./endpoints";
import type {
  CreateExerciseRequest,
  Id,
  Macros,
  SetInput,
  UpdateExerciseRequest,
  WorkoutRequest,
  WorkoutSessionRequest,
} from "./types";
import { startOfLocalDay } from "@/lib/dates";

export const keys = {
  me: ["me"] as const,
  stats: ["me", "stats"] as const,
  bodyweights: ["bodyweights"] as const,
  exercises: ["exercises"] as const,
  exercise: (id: Id) => ["exercises", id] as const,
  popular: ["exercises", "popular"] as const,
  catalog: ["catalog"] as const,
  logs: (exerciseId: Id) => ["logs", exerciseId] as const,
  previousLogs: (exerciseId: Id, limit: number) => ["logs", exerciseId, "previous", limit] as const,
  workouts: ["workouts"] as const,
  workout: (id: Id) => ["workouts", id] as const,
  programs: ["programs"] as const,
  nutritionEntries: ["nutrition", "entries"] as const,
  nutritionSummary: ["nutrition", "summary"] as const,
  analytics: ["analytics"] as const,
  weekly: ["analytics", "weekly"] as const,
  activity: ["analytics", "activity"] as const,
  records: ["analytics", "records"] as const,
};

// ---------- Users ----------

export const useStats = () => useQuery({ queryKey: keys.stats, queryFn: usersApi.stats });

// ---------- Body weight ----------

export const useBodyWeights = () =>
  useQuery({ queryKey: keys.bodyweights, queryFn: bodyWeightApi.list });

export function useAddBodyWeight() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (weight: number) => bodyWeightApi.create({ weight }),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.bodyweights }),
  });
}

export function useDeleteBodyWeight() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: bodyWeightApi.remove,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.bodyweights }),
  });
}

// ---------- Exercises ----------

export const useExercises = () => useQuery({ queryKey: keys.exercises, queryFn: exerciseApi.list });

export const useExercise = (id: Id) =>
  useQuery({ queryKey: keys.exercise(id), queryFn: () => exerciseApi.get(id) });

export const usePopularExercises = () =>
  useQuery({ queryKey: keys.popular, queryFn: () => exerciseApi.popular(5) });

export const useCatalog = () =>
  useQuery({ queryKey: keys.catalog, queryFn: exerciseApi.catalog, staleTime: Infinity });

export function useCreateExercise() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateExerciseRequest) => exerciseApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.exercises }),
  });
}

export function useUpdateExercise(id: Id) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateExerciseRequest) => exerciseApi.update(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.exercises });
      qc.invalidateQueries({ queryKey: keys.workouts });
    },
  });
}

export function useDeleteExercise() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: exerciseApi.remove,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.exercises });
      qc.invalidateQueries({ queryKey: keys.workouts });
      qc.invalidateQueries({ queryKey: keys.analytics });
    },
  });
}

// ---------- Logs ----------

export const useLogs = (exerciseId: Id) =>
  useQuery({ queryKey: keys.logs(exerciseId), queryFn: () => logApi.listForExercise(exerciseId) });

export const usePreviousLogs = (exerciseId: Id, limit: number) =>
  useQuery({
    queryKey: keys.previousLogs(exerciseId, limit),
    queryFn: () => logApi.previous(exerciseId, limit),
    enabled: limit > 0,
  });

function invalidateTraining(qc: ReturnType<typeof useQueryClient>, exerciseId?: Id) {
  if (exerciseId) qc.invalidateQueries({ queryKey: keys.logs(exerciseId) });
  else qc.invalidateQueries({ queryKey: ["logs"] });
  qc.invalidateQueries({ queryKey: keys.popular });
  qc.invalidateQueries({ queryKey: keys.analytics });
  qc.invalidateQueries({ queryKey: keys.stats });
}

export function useAddLog(exerciseId: Id) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (set: SetInput) => logApi.create(exerciseId, set),
    onSuccess: () => invalidateTraining(qc, exerciseId),
  });
}

export function useDeleteLog(exerciseId: Id) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: logApi.remove,
    onSuccess: () => invalidateTraining(qc, exerciseId),
  });
}

// ---------- Workouts ----------

export const useWorkouts = () => useQuery({ queryKey: keys.workouts, queryFn: workoutApi.list });

export const useWorkout = (id: Id) =>
  useQuery({ queryKey: keys.workout(id), queryFn: () => workoutApi.get(id) });

export function useCreateWorkout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: WorkoutRequest) => workoutApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.workouts }),
  });
}

export function useUpdateWorkout(id: Id) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: WorkoutRequest) => workoutApi.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.workouts }),
  });
}

export function useDeleteWorkout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: workoutApi.remove,
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.workouts }),
  });
}

export function useAddExerciseToWorkout(workoutId: Id) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (exerciseId: Id) => workoutApi.addExercise(workoutId, exerciseId),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.workouts }),
  });
}

export function useRemoveExerciseFromWorkout(workoutId: Id) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (workoutExerciseId: Id) => workoutApi.removeExercise(workoutId, workoutExerciseId),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.workouts }),
  });
}

export function useCompleteSession(workoutId: Id) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: WorkoutSessionRequest) => workoutApi.completeSession(workoutId, body),
    onSuccess: () => invalidateTraining(qc),
  });
}

// ---------- Programs ----------

export const usePrograms = () => useQuery({ queryKey: keys.programs, queryFn: programApi.list });

export function useCopyProgram() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: programApi.copy,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.workouts });
      qc.invalidateQueries({ queryKey: keys.exercises });
    },
  });
}

// ---------- Nutrition ----------

export const useNutritionEntries = () =>
  useQuery({ queryKey: keys.nutritionEntries, queryFn: nutritionApi.listEntries });

export const useNutritionSummary = () =>
  useQuery({
    queryKey: keys.nutritionSummary,
    queryFn: () => nutritionApi.summary(startOfLocalDay().toISOString()),
  });

function invalidateNutrition(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ["nutrition"] });
}

export function useAddNutritionEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Macros) => nutritionApi.createEntry(body),
    onSuccess: () => invalidateNutrition(qc),
  });
}

export function useDeleteNutritionEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: nutritionApi.removeEntry,
    onSuccess: () => invalidateNutrition(qc),
  });
}

export function useSetNutritionGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Macros) => nutritionApi.setGoal(body),
    onSuccess: () => invalidateNutrition(qc),
  });
}

// ---------- Analytics ----------

export const useWeeklyStats = () => useQuery({ queryKey: keys.weekly, queryFn: analyticsApi.weekly });
export const useActivity = () => useQuery({ queryKey: keys.activity, queryFn: analyticsApi.activity });
export const useRecords = () => useQuery({ queryKey: keys.records, queryFn: analyticsApi.records });
