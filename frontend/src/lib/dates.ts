/** UTC calendar day key, e.g. "2026-06-11". Matches the backend's analytics keys. */
export function dayKey(value: Date | string): string {
  const d = value instanceof Date ? value : new Date(value);
  return d.toISOString().slice(0, 10);
}

/** Midnight at the start of the user's local day. */
export function startOfLocalDay(now: Date = new Date()): Date {
  const d = new Date(now);
  d.setHours(0, 0, 0, 0);
  return d;
}

/** Short, locale-aware date for tables and chart labels. */
export function formatDate(value: Date | string): string {
  const d = value instanceof Date ? value : new Date(value);
  return d.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

/** Whole days between `date` and `now` (floored). */
export function daysAgo(date: Date | string, now: Date = new Date()): number {
  const d = date instanceof Date ? date : new Date(date);
  return Math.floor((now.getTime() - d.getTime()) / 86_400_000);
}
