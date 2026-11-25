import { useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { FaTrashAlt } from "react-icons/fa";
import {
  useAddExerciseToWorkout,
  useDeleteWorkout,
  useExercises,
  useRemoveExerciseFromWorkout,
  useUpdateWorkout,
  useWorkout,
} from "@/api/queries";
import type { Workout, WorkoutExercise } from "@/api/types";
import ConfirmDialog from "@/components/ConfirmDialog";
import EditableHeader from "@/components/EditableHeader";
import ErrorText from "@/components/ErrorText";
import Modal from "@/components/Modal";
import QueryState from "@/components/QueryState";
import { NAME_MAX_LENGTH } from "@/lib/validation";

function AddExerciseToWorkout({ workout }: { workout: Workout }) {
  const [open, setOpen] = useState(false);
  const [exerciseId, setExerciseId] = useState("");
  const [error, setError] = useState<unknown>(null);
  const exercises = useExercises();
  const add = useAddExerciseToWorkout(workout.id);

  const already = new Set(workout.exercises.map((we) => we.exerciseId));
  const available = (exercises.data ?? []).filter((e) => !already.has(e.id));

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!exerciseId) return setError("Please choose an exercise to add");
    setError(null);
    add.mutate(exerciseId, {
      onSuccess: () => {
        setOpen(false);
        setExerciseId("");
      },
      onError: setError,
    });
  }

  return (
    <div className="flex justify-center">
      <button onClick={() => setOpen(true)} className="btn-primary">Add exercise</button>
      <Modal open={open} onClose={() => setOpen(false)}>
        {exercises.isSuccess && exercises.data.length === 0 ? (
          <div className="flex flex-col gap-4">
            <h3 className="text-lg font-bold">No exercises available</h3>
            <p className="text-gray-400">Create some exercises first, then add them here.</p>
            <Link to="/dashboard/exercises" className="btn-primary">Go to exercises</Link>
          </div>
        ) : (
          <form autoComplete="off" className="flex flex-col gap-3" onSubmit={submit}>
            <h3 className="text-lg font-bold">Add an exercise to this workout</h3>
            <select aria-label="Exercise" value={exerciseId} onChange={(e) => setExerciseId(e.target.value)} className="input-field">
              <option value="">Choose an exercise</option>
              {available.map((ex) => (
                <option key={ex.id} value={ex.id} className="bg-sleek_gray">{ex.name}</option>
              ))}
            </select>
            <ErrorText error={error ?? exercises.error} />
            <button className="btn-primary" type="submit" disabled={add.isPending}>Submit</button>
          </form>
        )}
      </Modal>
    </div>
  );
}

function WorkoutExerciseRow({ workoutId, item }: { workoutId: string; item: WorkoutExercise }) {
  const [confirm, setConfirm] = useState(false);
  const remove = useRemoveExerciseFromWorkout(workoutId);
  return (
    <div className="flex items-center justify-between rounded-xl bg-white/[0.03] px-4 py-3">
      <Link to={`/dashboard/exercises/${item.exerciseId}`} className="text-base text-white hover:underline">{item.name}</Link>
      <button aria-label="Remove exercise" className="text-gray-500 transition hover:text-red-400" onClick={() => setConfirm(true)}>
        <FaTrashAlt size={16} />
      </button>
      <ConfirmDialog
        open={confirm}
        onClose={() => setConfirm(false)}
        onConfirm={() => remove.mutate(item.id, { onSuccess: () => setConfirm(false) })}
        title="Remove exercise from workout?"
        message="You can add it back anytime."
        confirmLabel="Remove"
        pending={remove.isPending}
        error={remove.error}
      />
    </div>
  );
}

export default function WorkoutDetailPage() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const workout = useWorkout(id);
  const update = useUpdateWorkout(id);
  const remove = useDeleteWorkout();

  return (
    <QueryState query={workout}>
      {(w) => (
        <section className="flex flex-col gap-6">
          <EditableHeader
            backTo="/dashboard/workouts"
            noun="workout"
            name={w.name}
            description={w.description}
            maxNameLength={NAME_MAX_LENGTH}
            onSave={(values) => update.mutateAsync(values)}
            onDelete={async () => {
              await remove.mutateAsync(id);
              navigate("/dashboard/workouts");
            }}
          />
          <AddExerciseToWorkout workout={w} />
          <div className="card">
            <h2 className="mb-4 text-lg font-semibold text-white">Exercises</h2>
            {w.exercises.length === 0 ? (
              <p className="text-gray-500">No exercises in this workout yet.</p>
            ) : (
              <div className="flex flex-col gap-2">
                {w.exercises.map((item) => (
                  <WorkoutExerciseRow key={item.id} workoutId={w.id} item={item} />
                ))}
              </div>
            )}
          </div>
        </section>
      )}
    </QueryState>
  );
}
