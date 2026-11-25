import { daysAgo } from "./dates";

export interface WeightPoint {
  weight: number;
  createdAt: string;
}

export interface WeightStats {
  highest: number;
  lowest: number;
  average: number;
}

/** Highest, lowest and average (1 d.p.) weight. All zero for no entries. */
export function weightStats(points: WeightPoint[]): WeightStats {
  if (points.length === 0) return { highest: 0, lowest: 0, average: 0 };
  const weights = points.map((p) => p.weight);
  const sum = weights.reduce((a, b) => a + b, 0);
  return {
    highest: Math.max(...weights),
    lowest: Math.min(...weights),
    average: Math.round((sum / weights.length) * 10) / 10,
  };
}

/** Weight of the entry whose age is closest to `days` days. Entries sorted oldest first. */
export function weightNDaysAgo(points: WeightPoint[], days: number, now = new Date()): number | null {
  let best: WeightPoint | null = null;
  let bestDiff = Infinity;
  for (const p of points) {
    const diff = Math.abs(days - daysAgo(p.createdAt, now));
    if (diff < bestDiff) {
      bestDiff = diff;
      best = p;
    }
  }
  return best?.weight ?? null;
}

/** Percentage change from `oldValue` to `newValue`, rounded to 2 d.p. */
export function percentChange(newValue: number, oldValue: number): number {
  if (oldValue === 0) return 0;
  return Math.round(((newValue - oldValue) / oldValue) * 10000) / 100;
}

/** Change of the latest weight vs. N days ago. Entries sorted oldest first. */
export function changeOver(points: WeightPoint[], days: number, now = new Date()): number {
  if (points.length === 0) return 0;
  const latest = points[points.length - 1].weight;
  const past = weightNDaysAgo(points, days, now);
  return past === null ? 0 : percentChange(latest, past);
}
