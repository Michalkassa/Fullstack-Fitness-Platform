import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { FaTrashAlt } from "react-icons/fa";
import { useAddLog, useDeleteExercise, useDeleteLog, useExercise, useLogs, useUpdateExercise } from "@/api/queries";
import type { Log } from "@/api/types";
import ConfirmDialog from "@/components/ConfirmDialog";
import EditableHeader from "@/components/EditableHeader";
import ErrorText from "@/components/ErrorText";
import LineChart from "@/components/LineChart";
import Modal from "@/components/Modal";
import QueryState from "@/components/QueryState";
import { formatDate } from "@/lib/dates";
import { NAME_MAX_LENGTH, validateSet } from "@/lib/validation";

function AddLog({ exerciseId }: { exerciseId: string }) {
  const [open, setOpen] = useState(false);
  const [weight, setWeight] = useState("");
  const [reps, setReps] = useState("");
  const [error, setError] = useState<unknown>(null);
  const add = useAddLog(exerciseId);

  function submit(e: FormEvent) {
    e.preventDefault();
    const problem = validateSet({ weight, reps });
    if (problem) return setError(problem);
    setError(null);
    add.mutate(
      { weight: Number(weight), reps: Number(reps) },
      {
        onSuccess: () => {
          setOpen(false);
          setWeight("");
          setReps("");
        },
        onError: setError,
      },
    );
  }

  return (
    <div className="flex justify-center">
      <button onClick={() => setOpen(true)} className="btn-primary">Add new log</button>
      <Modal open={open} onClose={() => setOpen(false)}>
        <form autoComplete="off" onSubmit={submit} className="flex flex-col gap-3">
          <h3 className="text-lg font-bold">Add new log</h3>
          <input type="number" step="0.01" min="0" aria-label="Weight in kilograms" placeholder="Weight (kg)"
            className="input-field" value={weight} onChange={(e) => setWeight(e.target.value)} autoFocus />
          <input type="number" min="1" step="1" aria-label="Repetitions" placeholder="Repetitions"
            className="input-field" value={reps} onChange={(e) => setReps(e.target.value)} />
          <ErrorText error={error} />
          <button className="btn-primary" type="submit" disabled={add.isPending}>Submit</button>
        </form>
      </Modal>
    </div>
  );
}

function LogRow({ log }: { log: Log }) {
  const [confirm, setConfirm] = useState(false);
  const remove = useDeleteLog(log.exerciseId);
  return (
    <tr>
      <td className="text-gray-400">{formatDate(log.createdAt)}</td>
      <td>{log.weight}</td>
      <td>{log.reps}</td>
      <td>{log.oneRepMax}</td>
      <td className="text-right">
        <button aria-label="Delete log" className="text-gray-500 transition hover:text-red-400" onClick={() => setConfirm(true)}>
          <FaTrashAlt size={16} />
        </button>
        <ConfirmDialog
          open={confirm}
          onClose={() => setConfirm(false)}
          onConfirm={() => remove.mutate(log.id, { onSuccess: () => setConfirm(false) })}
          title="Delete log?"
          pending={remove.isPending}
          error={remove.error}
        />
      </td>
    </tr>
  );
}

export default function ExerciseDetailPage() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const exercise = useExercise(id);
  const logs = useLogs(id);
  const update = useUpdateExercise(id);
  const remove = useDeleteExercise();

  return (
    <QueryState query={exercise}>
      {(ex) => (
        <section className="flex flex-col gap-6">
          <EditableHeader
            backTo="/dashboard/exercises"
            noun="exercise"
            name={ex.name}
            description={ex.description}
            maxNameLength={NAME_MAX_LENGTH}
            deleteMessage="This can’t be undone — all logs for this exercise are removed too."
            onSave={(values) => update.mutateAsync(values)}
            onDelete={async () => {
              await remove.mutateAsync(id);
              navigate("/dashboard/exercises");
            }}
          />

          <div className="card">
            <h2 className="mb-4 text-lg font-semibold text-white">1RM Progress</h2>
            <div className="h-56">
              <QueryState
                query={logs}
                empty={<div className="flex h-full items-center justify-center text-gray-500">No logs yet.</div>}
              >
                {(data) => (
                  <LineChart label="Estimated 1RM" labels={data.map((l) => formatDate(l.createdAt))} values={data.map((l) => l.oneRepMax)} />
                )}
              </QueryState>
            </div>
          </div>

          <AddLog exerciseId={id} />

          <div className="card">
            <QueryState query={logs} empty={<p className="text-gray-500">No logs yet — add your first set above.</p>}>
              {(data) => (
                <div className="table-wrap">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Weight (kg)</th>
                        <th>Reps</th>
                        <th>Est. 1RM</th>
                        <th className="text-right">Remove</th>
                      </tr>
                    </thead>
                    <tbody>
                      {[...data].reverse().map((log) => (
                        <LogRow key={log.id} log={log} />
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </QueryState>
          </div>
        </section>
      )}
    </QueryState>
  );
}
