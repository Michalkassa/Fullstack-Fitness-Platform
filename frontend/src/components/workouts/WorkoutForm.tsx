import { useState, type FormEvent } from "react";
import { useCreateWorkout } from "@/api/queries";
import ErrorText from "@/components/ErrorText";
import Modal from "@/components/Modal";
import { NAME_MAX_LENGTH, validateNamed } from "@/lib/validation";

export default function AddWorkout() {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<unknown>(null);
  const create = useCreateWorkout();

  function submit(e: FormEvent) {
    e.preventDefault();
    const problem = validateNamed({ name, description });
    if (problem) return setError(problem);
    setError(null);
    create.mutate(
      { name: name.trim(), description: description.trim() },
      {
        onSuccess: () => {
          setOpen(false);
          setName("");
          setDescription("");
        },
        onError: setError,
      },
    );
  }

  return (
    <>
      <button onClick={() => setOpen(true)} className="btn-primary">Add workout</button>
      <Modal open={open} onClose={() => setOpen(false)}>
        <form autoComplete="off" onSubmit={submit} className="flex flex-col gap-3">
          <h3 className="text-lg font-bold">Add workout</h3>
          <input aria-label="Workout name" placeholder="Name" maxLength={NAME_MAX_LENGTH} className="input-field"
            value={name} onChange={(e) => setName(e.target.value)} autoFocus />
          <textarea aria-label="Workout description" placeholder="Description..." className="input-field h-36"
            value={description} onChange={(e) => setDescription(e.target.value)} />
          <ErrorText error={error} />
          <button className="btn-primary" type="submit" disabled={create.isPending}>Submit</button>
        </form>
      </Modal>
    </>
  );
}
