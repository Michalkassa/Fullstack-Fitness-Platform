import { useState, type FormEvent } from "react";
import { FaArrowDown, FaArrowUp, FaTrashAlt } from "react-icons/fa";
import { useAddBodyWeight, useBodyWeights, useDeleteBodyWeight } from "@/api/queries";
import type { BodyWeight } from "@/api/types";
import BodyWeightChart from "@/components/bodyweight/BodyWeightChart";
import ConfirmDialog from "@/components/ConfirmDialog";
import ErrorText from "@/components/ErrorText";
import Modal from "@/components/Modal";
import QueryState from "@/components/QueryState";
import { changeOver, weightStats } from "@/lib/bodyweight";
import { formatDate } from "@/lib/dates";
import { validatePositive } from "@/lib/validation";

function AddBodyWeight() {
  const [open, setOpen] = useState(false);
  const [weight, setWeight] = useState("");
  const [error, setError] = useState<unknown>(null);
  const add = useAddBodyWeight();

  function submit(e: FormEvent) {
    e.preventDefault();
    const problem = validatePositive(weight, "Please enter a weight");
    if (problem) return setError(problem);
    setError(null);
    add.mutate(Number(weight), {
      onSuccess: () => {
        setOpen(false);
        setWeight("");
      },
      onError: setError,
    });
  }

  return (
    <>
      <button onClick={() => setOpen(true)} className="btn-primary">Add weight</button>
      <Modal open={open} onClose={() => setOpen(false)}>
        <form autoComplete="off" onSubmit={submit} className="flex flex-col gap-3">
          <h3 className="text-lg font-bold">Add new body weight</h3>
          <div className="flex gap-3">
            <input type="number" step="0.01" min="0" aria-label="Body weight in kilograms" placeholder="Weight (kg)"
              className="input-field" value={weight} onChange={(e) => setWeight(e.target.value)} autoFocus />
            <button className="btn-primary" type="submit" disabled={add.isPending}>Submit</button>
          </div>
          <ErrorText error={error} />
        </form>
      </Modal>
    </>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <p className="text-xs text-gray-400">{label}</p>
      <p className="text-3xl">{value}</p>
    </div>
  );
}

function Change({ label, value, hasData }: { label: string; value: number; hasData: boolean }) {
  const color = !hasData ? "text-gray-500" : value >= 0 ? "text-green-500" : "text-red-500";
  return (
    <div>
      <p className="text-xs text-gray-400">{label}</p>
      <p className={`flex items-center gap-1 text-3xl ${color}`}>
        {value}%{hasData && (value >= 0 ? <FaArrowUp size={18} /> : <FaArrowDown size={18} />)}
      </p>
    </div>
  );
}

function BodyWeightRow({ entry }: { entry: BodyWeight }) {
  const [confirm, setConfirm] = useState(false);
  const remove = useDeleteBodyWeight();
  return (
    <tr>
      <td className="text-gray-400">{formatDate(entry.createdAt)}</td>
      <td>{entry.weight}</td>
      <td className="text-right">
        <button aria-label="Delete bodyweight" className="text-gray-500 transition hover:text-red-400" onClick={() => setConfirm(true)}>
          <FaTrashAlt size={16} />
        </button>
        <ConfirmDialog
          open={confirm}
          onClose={() => setConfirm(false)}
          onConfirm={() => remove.mutate(entry.id, { onSuccess: () => setConfirm(false) })}
          title="Delete entry?"
          pending={remove.isPending}
          error={remove.error}
        />
      </td>
    </tr>
  );
}

export default function BodyWeightPage() {
  const query = useBodyWeights();
  const data = query.data ?? [];
  const stats = weightStats(data);
  const hasData = data.length > 0;

  return (
    <section className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="page-header">Body Weight</h1>
        <AddBodyWeight />
      </div>

      <div className="card">
        <h2 className="text-lg font-semibold text-white">Progress</h2>
        <div className="mt-4 h-56">
          <BodyWeightChart />
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="card flex justify-between gap-5 md:gap-12">
          <Stat label="Highest" value={stats.highest} />
          <Stat label="Lowest" value={stats.lowest} />
          <Stat label="Average" value={stats.average} />
        </div>
        <div className="card flex justify-evenly gap-5 md:gap-10">
          <Change label="in 7 days" value={changeOver(data, 7)} hasData={hasData} />
          <Change label="in 30 days" value={changeOver(data, 30)} hasData={hasData} />
          <Change label="in 1 year" value={changeOver(data, 365)} hasData={hasData} />
        </div>
      </div>

      <div className="card">
        <QueryState query={query} empty={<p className="text-gray-500">No bodyweight entries yet.</p>}>
          {(entries) => (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Weight (kg)</th>
                    <th className="text-right">Remove</th>
                  </tr>
                </thead>
                <tbody>
                  {[...entries].reverse().map((entry) => (
                    <BodyWeightRow key={entry.id} entry={entry} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </QueryState>
      </div>
    </section>
  );
}
