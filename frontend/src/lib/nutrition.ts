/** Atwater energy factors: kcal per gram of each macronutrient. */
export const PROTEIN_KCAL_PER_G = 4;
export const CARB_KCAL_PER_G = 4;
export const FAT_KCAL_PER_G = 9;

/** Estimated calories from macro grams, rounded to the nearest kcal. */
export function caloriesFromMacros(protein: number, carbs: number, fat: number): number {
  const p = Number.isFinite(protein) ? protein : 0;
  const c = Number.isFinite(carbs) ? carbs : 0;
  const f = Number.isFinite(fat) ? fat : 0;
  return Math.round(p * PROTEIN_KCAL_PER_G + c * CARB_KCAL_PER_G + f * FAT_KCAL_PER_G);
}

/** Percent of goal consumed, clamped to 0-100. */
export function percentOf(consumed: number, goal: number): number {
  return goal > 0 ? Math.min(100, Math.round((consumed / goal) * 100)) : 0;
}
