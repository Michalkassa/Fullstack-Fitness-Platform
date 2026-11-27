import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { FaTrashAlt } from "react-icons/fa";
import { useCompleteSession, usePreviousLogs, useWorkout } from "@/api/queries";
import type { WorkoutExercise, WorkoutSessionResult } from "@/api/types";
import ErrorText from "@/components/ErrorText";
import Modal from "@/components/Modal";
import QueryState from "@/components/QueryState";
import {
  clearDraft,
  emptySet,
  exceedsRepCap,
  formatElapsed,
  loadDraft,
  saveDraft,
  toSessionRequest,
  type DraftSet,
  type SessionDraft,
} from "@/lib/session";
import { MAX_REPS } from "@/lib/validation";

function Timer({ startedAt }: { startedAt: number }) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);
  return <span className="font-mono text-2xl tabular-nums text-gray-300">{formatElapsed(now - startedAt)}</span>;
}

interface ExerciseSetsProps {
  item: WorkoutExercise;
  sets: DraftSet[];
  onChange: (sets: DraftSet[]) => void;
}

function ExerciseSets({ item, sets, onChange }: ExerciseSetsProps) {
  const previous = usePreviousLogs(item.exerciseId, sets.length);
  // The API returns the most recent sets first; show them oldest-first so set 1 lines up with set 1.
  const prev = [...(previous.data ?? [])].reverse();

  const update = (i: number, patch: Partial<DraftSet>) =>
    onChange(sets.map((s, idx) => (idx === i ? { ...s, ...patch } : s)));

  return (
    <div className="card flex flex-col gap-3">
      <p className="text-lg font-semibold text-white">{item.name}</p>
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Set</th>
              <th>Previous</th>
              <th>kg</th>
              <th>Reps</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {sets.map((set, i) => (
              <tr key={i}>
                <td className="text-gray-400">{i + 1}</td>
                <td className="text-gray-500">{prev[i] ? `${prev[i].weight} kg × ${prev[i].reps}` : "—"}</td>
                <td>
                  <input type="number" min="0" step="0.5" aria-label={`Set ${i + 1} weight in kilograms`}
                    placeholder={prev[i] ? String(prev[i].weight) : "0"} value={set.weight}
                    onChange={(e) => update(i, { weight: e.target.value })} className="input-field max-w-24 px-2 py-1 text-center" />
                </td>
                <td>
                  <input type="number" min="1" max={MAX_REPS} step="1" aria-label={`Set ${i + 1} repetitions`}
                    placeholder={prev[i] ? String(prev[i].reps) : "0"} value={set.reps}
                    onChange={(e) => update(i, { reps: e.target.value })} className="input-field max-w-24 px-2 py-1 text-center" />
                </td>
                <td className="text-right">
                  <button aria-label={`Delete set ${i + 1}`} className="text-gray-500 transition hover:text-red-400"
                    onClick={() => onChange(sets.filter((_, idx) => idx !== i))}>
                    <FaTrashAlt size={14} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <button className="btn-ghost self-start" onClick={() => onChange([...sets, emptySet()])}>+ Add set</button>
    </div>
  );
}

export default function RunningWorkoutPage() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const workout = useWorkout(id);
  const complete = useCompleteSession(id);
  const [draft, setDraft] = useState<SessionDraft>(() => loadDraft(id));
  const [result, setResult] = useState<WorkoutSessionResult | null>(null);
  const [error, setError] = useState<unknown>(null);

  // Once the session is submitted the draft is cleared and must not be re-saved.
  useEffect(() => {
    if (!result) saveDraft(id, draft);
  }, [id, draft, result]);

  const setsFor = (exerciseId: string) => draft.sets[exerciseId] ?? [emptySet()];

  function finish() {
    if (!workout.data) return;
    const sets = Object.fromEntries(workout.data.exercises.map((e) => [e.exerciseId, setsFor(e.exerciseId)]));
    if (exceedsRepCap(sets)) return setError(`At most ${MAX_REPS} reps per set. Lower the reps before finishing.`);
    const body = toSessionRequest({ ...draft, sets }, workout.data.exercises.map((e) => e.exerciseId));
    if (body.exercises.length === 0) return setError("Enter at least one set (weight and reps) before finishing.");
    setError(null);
    complete.mutate(body, {
      onSuccess: (res) => {
        clearDraft(id);
        setResult(res);
      },
      onError: setError,
    });
  }

  function discard() {
    clearDraft(id);
    setDraft({ startedAt: Date.now(), sets: {} });
  }

  return (
    <QueryState query={workout}>
      {(w) => (
        <section className="flex flex-col gap-6 text-white">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h1 className="page-header">{w.name}</h1>
            <Timer startedAt={draft.startedAt} />
          </div>

          <div className="flex flex-wrap gap-3">
            <button onClick={finish} className="btn-success" disabled={complete.isPending}>
              {complete.isPending ? "Saving…" : "Finish workout"}
            </button>
            <button onClick={discard} className="btn-ghost">Reset</button>
          </div>
          <ErrorText error={error} />

          {w.exercises.length === 0 ? (
            <p className="text-gray-400">
              This workout has no exercises.{" "}
              <Link className="text-white underline" to={`/dashboard/workouts/${w.id}`}>Add some</Link>.
            </p>
          ) : (
            w.exercises.map((item) => (
              <ExerciseSets
                key={item.id}
                item={item}
                sets={setsFor(item.exerciseId)}
                onChange={(sets) => setDraft((d) => ({ ...d, sets: { ...d.sets, [item.exerciseId]: sets } }))}
              />
            ))
          )}

          <Modal open={result !== null} onClose={() => navigate("/dashboard")}>
            <div className="flex flex-col gap-4">
              <h3 className="text-lg font-bold">Workout complete</h3>
              <p className="text-gray-400">
                Logged {result?.logsCreated} set{result?.logsCreated === 1 ? "" : "s"} and earned{" "}
                <span className="font-semibold text-white">{result?.xpAwarded} XP</span>.
              </p>
              <Link to="/dashboard" className="btn-primary">Back to dashboard</Link>
            </div>
          </Modal>
        </section>
      )}
    </QueryState>
  );
}
