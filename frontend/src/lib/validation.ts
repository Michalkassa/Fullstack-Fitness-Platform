/**
 * Client-side form checks for instant feedback. The backend is the source of truth
 * and must enforce the same rules (see docs/API.md, "Validation rules").
 * Each function returns the first problem as a user-facing message, or null.
 */

export const NAME_MAX_LENGTH = 40;
export const PASSWORD_MIN_LENGTH = 8;

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateLogin(v: { email: string; password: string }): string | null {
  if (!v.email.trim()) return "Please enter an email";
  if (!EMAIL_RE.test(v.email.trim())) return "Not a valid email";
  if (!v.password) return "Please enter a password";
  return null;
}

export function validateRegister(v: { email: string; password: string; confirmPassword: string }): string | null {
  if (!v.email.trim()) return "Please enter an email";
  if (!EMAIL_RE.test(v.email.trim())) return "Not a valid email";
  if (v.password.length < PASSWORD_MIN_LENGTH) return `Password must be at least ${PASSWORD_MIN_LENGTH} characters`;
  if (v.password !== v.confirmPassword) return "Passwords do not match";
  return null;
}

export function validateNamed(v: { name: string; description: string }): string | null {
  if (!v.name.trim() || !v.description.trim()) return "Enter a name and a description";
  if (v.name.trim().length > NAME_MAX_LENGTH) return `Name must be at most ${NAME_MAX_LENGTH} characters`;
  return null;
}

export function validatePositive(value: string, message: string): string | null {
  const n = Number(value);
  return value.trim() === "" || !Number.isFinite(n) || n <= 0 ? message : null;
}

export function validateSet(v: { weight: string; reps: string }): string | null {
  const message = "Enter a weight and the reps";
  const weight = Number(v.weight);
  const reps = Number(v.reps);
  if (v.weight.trim() === "" || !Number.isFinite(weight) || weight < 0) return message;
  if (v.reps.trim() === "" || !Number.isInteger(reps) || reps <= 0) return message;
  return null;
}

export function validateMacros(v: { calories: string; protein: string; carbs: string; fat: string }): string | null {
  const calories = Number(v.calories);
  if (v.calories.trim() === "" || !Number.isFinite(calories)) return "Enter a calorie amount";
  if (calories < 0) return "Calories cannot be negative";
  for (const m of [v.protein, v.carbs, v.fat]) {
    if (m.trim() !== "" && (!Number.isFinite(Number(m)) || Number(m) < 0)) return "Macros cannot be negative";
  }
  return null;
}
