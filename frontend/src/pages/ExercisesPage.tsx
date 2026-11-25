import { useMemo, useState, type FormEvent } from "react";
import { Link } from "react-router";
import Select from "react-select";
import { useCatalog, useCreateExercise, useExercises } from "@/api/queries";
import type { CatalogExercise } from "@/api/types";
import ErrorText from "@/components/ErrorText";
import Modal from "@/components/Modal";
import QueryState from "@/components/QueryState";
import { NAME_MAX_LENGTH, validateNamed } from "@/lib/validation";

type Option = { value: string; label: string; item: CatalogExercise };
type Group = { label: string; options: Option[] };

function AddExercise() {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [muscleGroup, setMuscleGroup] = useState("");
  const [equipment, setEquipment] = useState("");
  const [error, setError] = useState<unknown>(null);
  const catalog = useCatalog();
  const create = useCreateExercise();

  const groups = useMemo<Group[]>(() => {
    const byGroup = new Map<string, Option[]>();
    for (const item of catalog.data ?? []) {
      const list = byGroup.get(item.muscleGroup) ?? [];
      list.push({ value: item.id, label: item.name, item });
      byGroup.set(item.muscleGroup, list);
    }
    return Array.from(byGroup, ([label, options]) => ({ label, options }));
  }, [catalog.data]);

  function reset() {
    setName("");
    setDescription("");
    setMuscleGroup("");
    setEquipment("");
    setError(null);
  }

  function pick(option: Option | null) {
    if (!option) return;
    const { item } = option;
    setName(item.name.slice(0, NAME_MAX_LENGTH));
    setDescription(item.instructions);
    setMuscleGroup(item.muscleGroup);
    setEquipment(item.equipment);
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    const problem = validateNamed({ name, description });
    if (problem) return setError(problem);
    setError(null);
    create.mutate(
      { name: name.trim(), description: description.trim(), muscleGroup: muscleGroup || null, equipment: equipment || null },
      {
        onSuccess: () => {
          setOpen(false);
          reset();
        },
        onError: setError,
      },
    );
  }

  return (
    <>
      <button onClick={() => setOpen(true)} className="btn-primary">Add exercise</button>
      <Modal open={open} onClose={() => setOpen(false)}>
        <form autoComplete="off" onSubmit={submit} className="flex flex-col gap-3">
          <h3 className="text-lg font-bold">Add exercise</h3>
          <label htmlFor="catalog" className="text-sm text-gray-400">Pick from library (optional)</label>
          <Select<Option, false, Group>
            inputId="catalog"
            options={groups}
            onChange={pick}
            isClearable
            isLoading={catalog.isPending}
            placeholder="Search the exercise library..."
            className="text-black"
          />
          <input aria-label="Exercise name" placeholder="Name" maxLength={NAME_MAX_LENGTH} value={name}
            onChange={(e) => setName(e.target.value)} className="input-field" />
          <textarea aria-label="Exercise description" placeholder="Description..." value={description}
            onChange={(e) => setDescription(e.target.value)} className="input-field h-36" />
          {muscleGroup && (
            <p className="text-sm text-gray-400">
              {muscleGroup}
              {equipment ? ` · ${equipment}` : ""}
            </p>
          )}
          <ErrorText error={error} />
          <button className="btn-primary" type="submit" disabled={create.isPending}>
            {create.isPending ? "Saving…" : "Submit"}
          </button>
        </form>
      </Modal>
    </>
  );
}

export default function ExercisesPage() {
  const query = useExercises();
  return (
    <section className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="page-header">Exercises</h1>
        <AddExercise />
      </div>
      <QueryState query={query} empty={<p className="text-center text-gray-400">No exercises created yet.</p>}>
        {(exercises) => (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {exercises.map((exercise) => (
              <Link
                key={exercise.id}
                to={`/dashboard/exercises/${exercise.id}`}
                className="card block transition hover:border-white/10 hover:bg-white/[0.05]"
              >
                <div className="flex items-start justify-between gap-2">
                  <h3 className="text-lg font-semibold text-white">{exercise.name}</h3>
                  {exercise.muscleGroup && <span className="chip shrink-0">{exercise.muscleGroup}</span>}
                </div>
                <p className="line-clamp-3-safe mt-1 text-sm text-gray-400">{exercise.description}</p>
              </Link>
            ))}
          </div>
        )}
      </QueryState>
    </section>
  );
}
