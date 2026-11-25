import { useState, type FormEvent } from "react";
import type { Macros } from "@/api/types";
import ErrorText from "@/components/ErrorText";
import { caloriesFromMacros } from "@/lib/nutrition";
import { validateMacros } from "@/lib/validation";

interface MacroFormProps {
  title: string;
  submitLabel: string;
  initial?: Macros | null;
  /** Start with calories auto-calculated from macros. */
  autoByDefault: boolean;
  pending: boolean;
  onSubmit: (values: Macros) => Promise<unknown>;
}

const str = (n: number | undefined) => (n === undefined || n === 0 ? "" : String(n));

/** Calories + macros form with optional "calculate calories from macros" mode. */
export default function MacroForm({ title, submitLabel, initial, autoByDefault, pending, onSubmit }: MacroFormProps) {
  const [auto, setAuto] = useState(autoByDefault);
  const [protein, setProtein] = useState(str(initial?.protein));
  const [carbs, setCarbs] = useState(str(initial?.carbs));
  const [fat, setFat] = useState(str(initial?.fat));
  const [calories, setCalories] = useState(initial ? String(initial.calories) : "");
  const [error, setError] = useState<unknown>(null);

  const hasMacros = protein !== "" || carbs !== "" || fat !== "";
  const computed = caloriesFromMacros(Number(protein), Number(carbs), Number(fat));
  const caloriesValue = auto ? (hasMacros ? String(computed) : "") : calories;

  async function submit(e: FormEvent) {
    e.preventDefault();
    const values = { calories: caloriesValue, protein, carbs, fat };
    const problem = validateMacros(values);
    if (problem) return setError(problem);
    setError(null);
    try {
      await onSubmit({
        calories: Math.round(Number(caloriesValue)),
        protein: Number(protein || 0),
        carbs: Number(carbs || 0),
        fat: Number(fat || 0),
      });
    } catch (err) {
      setError(err);
    }
  }

  return (
    <form autoComplete="off" onSubmit={submit} className="flex flex-col gap-3">
      <h3 className="text-lg font-bold">{title}</h3>
      <input value={protein} onChange={(e) => setProtein(e.target.value)} type="number" step="0.1" min="0"
        aria-label="Protein in grams" placeholder="Protein (g)" className="input-field" />
      <input value={carbs} onChange={(e) => setCarbs(e.target.value)} type="number" step="0.1" min="0"
        aria-label="Carbohydrates in grams" placeholder="Carbs (g)" className="input-field" />
      <input value={fat} onChange={(e) => setFat(e.target.value)} type="number" step="0.1" min="0"
        aria-label="Fat in grams" placeholder="Fat (g)" className="input-field" />
      <label className="flex items-center gap-2 text-sm text-gray-400">
        <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} className="accent-atlantis_blue" />
        Calculate calories from macros (4 / 4 / 9 kcal·g)
      </label>
      <input
        value={caloriesValue}
        onChange={(e) => setCalories(e.target.value)}
        readOnly={auto}
        type="number"
        min="0"
        aria-label="Calories"
        placeholder="Calories (kcal)"
        className={`input-field ${auto ? "cursor-not-allowed text-gray-300" : ""}`}
      />
      <ErrorText error={error} />
      <button className="btn-primary" type="submit" disabled={pending}>
        {pending ? "Saving…" : submitLabel}
      </button>
    </form>
  );
}
