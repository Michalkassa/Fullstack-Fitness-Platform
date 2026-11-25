/**
 * In-progress workout session state, persisted to localStorage so a refresh or
 * accidental navigation doesn't lose the sets you've entered.
 */
import type { WorkoutSessionRequest } from "@/api/types";

export interface DraftSet {
  weight: string;
  reps: string;
}

export interface SessionDraft {
  startedAt: number;
  /** exerciseId -> sets */
  sets: Record<string, DraftSet[]>;
}

const key = (workoutId: string) => `lockedin.session.${workoutId}`;

export const emptySet = (): DraftSet => ({ weight: "", reps: "" });

export function loadDraft(workoutId: string, now = Date.now()): SessionDraft {
  try {
    const raw = localStorage.getItem(key(workoutId));
    if (raw) {
      const parsed = JSON.parse(raw) as SessionDraft;
      if (typeof parsed.startedAt === "number" && parsed.sets && typeof parsed.sets === "object") return parsed;
    }
  } catch {
    // corrupted -> start fresh
  }
  return { startedAt: now, sets: {} };
}

export function saveDraft(workoutId: string, draft: SessionDraft): void {
  localStorage.setItem(key(workoutId), JSON.stringify(draft));
}

export function clearDraft(workoutId: string): void {
  localStorage.removeItem(key(workoutId));
}

/** Only sets with a valid weight (>= 0) and positive whole reps are submitted. */
export function toSessionRequest(draft: SessionDraft, exerciseOrder: string[]): WorkoutSessionRequest {
  const exercises = exerciseOrder
    .map((exerciseId) => ({
      exerciseId,
      sets: (draft.sets[exerciseId] ?? [])
        .filter((s) => s.weight.trim() !== "" && s.reps.trim() !== "")
        .map((s) => ({ weight: Number(s.weight), reps: Number(s.reps) }))
        .filter((s) => Number.isFinite(s.weight) && s.weight >= 0 && Number.isInteger(s.reps) && s.reps > 0),
    }))
    .filter((e) => e.sets.length > 0);
  return { exercises };
}

/** "hh:mm:ss" for an elapsed number of milliseconds. */
export function formatElapsed(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return [h, m, s].map((n) => String(n).padStart(2, "0")).join(":");
}
