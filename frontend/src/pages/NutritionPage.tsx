import { useState } from "react";
import { FaTrashAlt } from "react-icons/fa";
import {
  useAddNutritionEntry,
  useDeleteNutritionEntry,
  useNutritionEntries,
  useNutritionSummary,
  useSetNutritionGoal,
} from "@/api/queries";
import type { NutritionEntry, NutritionGoal } from "@/api/types";
import ConfirmDialog from "@/components/ConfirmDialog";
import LineChart from "@/components/LineChart";
import Modal from "@/components/Modal";
import QueryState from "@/components/QueryState";
import MacroForm from "@/components/nutrition/MacroForm";
import { formatDate } from "@/lib/dates";
import { percentOf } from "@/lib/nutrition";

function AddEntry() {
  const [open, setOpen] = useState(false);
  const add = useAddNutritionEntry();
  return (
    <>
      <button onClick={() => setOpen(true)} className="btn-primary">Add entry</button>
      <Modal open={open} onClose={() => setOpen(false)}>
        {/* key remounts the form so it's empty each time it opens */}
        <MacroForm
          key={String(open)}
          title="Log nutrition"
          submitLabel="Submit"
          autoByDefault
          pending={add.isPending}
          onSubmit={async (values) => {
            await add.mutateAsync(values);
            setOpen(false);
          }}
        />
      </Modal>
    </>
  );
}

function SetGoalButton({ goal }: { goal: NutritionGoal | null }) {
  const [open, setOpen] = useState(false);
  const save = useSetNutritionGoal();
  return (
    <>
      <button onClick={() => setOpen(true)} className={goal ? "btn-ghost" : "btn-primary"}>
        {goal ? "Edit goal" : "Set goal"}
      </button>
      <Modal open={open} onClose={() => setOpen(false)}>
        <MacroForm
          key={String(open)}
          title="Daily goal"
          submitLabel="Save goal"
          initial={goal}
          // Editing keeps the saved calories; a brand-new goal auto-calculates them.
          autoByDefault={!goal}
          pending={save.isPending}
          onSubmit={async (values) => {
            await save.mutateAsync(values);
            setOpen(false);
          }}
        />
      </Modal>
    </>
  );
}

function MacroBar({ label, consumed, goal }: { label: string; consumed: number; goal: number }) {
  return (
    <div>
      <div className="flex items-baseline justify-between text-sm">
        <span className="text-gray-400">{label}</span>
        <span className="font-medium text-white">{Math.round(Math.max(0, goal - consumed))}g left</span>
      </div>
      <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-white/10">
        <div className="h-full rounded-full bg-atlantis_blue transition-all" style={{ width: `${percentOf(consumed, goal)}%` }} />
      </div>
      <p className="mt-1 text-xs text-gray-500">
        {Math.round(consumed)} / {Math.round(goal)} g
      </p>
    </div>
  );
}

function GoalCard() {
  const query = useNutritionSummary();
  return (
    <QueryState query={query}>
      {({ goal, consumed }) => {
        if (!goal) {
          return (
            <div className="card flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
              <div>
                <h2 className="text-lg font-semibold text-white">Daily goal</h2>
                <p className="mt-1 text-gray-500">Set a daily calorie &amp; macro goal to track what’s remaining.</p>
              </div>
              <SetGoalButton goal={null} />
            </div>
          );
        }
        const remaining = goal.calories - consumed.calories;
        const showMacros = goal.protein > 0 || goal.carbs > 0 || goal.fat > 0;
        return (
          <div className="card">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-white">Today</h2>
              <SetGoalButton goal={goal} />
            </div>
            <div className="mt-4 flex items-end gap-2">
              <span className={`text-4xl font-semibold ${remaining < 0 ? "text-red-400" : "text-white"}`}>
                {Math.round(Math.abs(remaining))}
              </span>
              <span className="mb-1 text-gray-500">kcal {remaining < 0 ? "over" : "remaining"}</span>
            </div>
            <p className="mt-1 text-xs text-gray-500">
              {Math.round(consumed.calories)} of {goal.calories} kcal consumed
            </p>
            <div className="mt-2 h-2.5 overflow-hidden rounded-full bg-white/10">
              <div className="h-full rounded-full bg-atlantis_blue transition-all" style={{ width: `${percentOf(consumed.calories, goal.calories)}%` }} />
            </div>
            {showMacros && (
              <div className="mt-6 grid gap-5 sm:grid-cols-3">
                <MacroBar label="Protein" consumed={consumed.protein} goal={goal.protein} />
                <MacroBar label="Carbs" consumed={consumed.carbs} goal={goal.carbs} />
                <MacroBar label="Fat" consumed={consumed.fat} goal={goal.fat} />
              </div>
            )}
          </div>
        );
      }}
    </QueryState>
  );
}

function EntryRow({ entry }: { entry: NutritionEntry }) {
  const [confirm, setConfirm] = useState(false);
  const remove = useDeleteNutritionEntry();
  return (
    <tr>
      <td className="text-gray-400">{formatDate(entry.createdAt)}</td>
      <td>{entry.calories} kcal</td>
      <td>{entry.protein} g</td>
      <td>{entry.carbs} g</td>
      <td>{entry.fat} g</td>
      <td className="text-right">
        <button aria-label="Delete entry" className="text-gray-500 transition hover:text-red-400" onClick={() => setConfirm(true)}>
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

export default function NutritionPage() {
  const entries = useNutritionEntries();
  return (
    <section className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="page-header">Nutrition</h1>
        <AddEntry />
      </div>
      <GoalCard />
      <div className="card">
        <QueryState query={entries} empty={<p className="text-center text-gray-400">No nutrition logged yet.</p>}>
          {(data) => (
            <div className="flex flex-col gap-4 text-white">
              <div className="h-56">
                <LineChart label="Calories" labels={data.map((e) => formatDate(e.createdAt))} values={data.map((e) => e.calories)} />
              </div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Calories</th>
                      <th>Protein</th>
                      <th>Carbs</th>
                      <th>Fat</th>
                      <th className="text-right">Remove</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...data].reverse().map((e) => (
                      <EntryRow key={e.id} entry={e} />
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </QueryState>
      </div>
    </section>
  );
}
