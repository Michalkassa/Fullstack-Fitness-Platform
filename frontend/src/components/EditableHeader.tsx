import { useState } from "react";
import { Link } from "react-router";
import { FaLongArrowAltLeft, FaPen, FaRegTrashAlt } from "react-icons/fa";
import Modal from "./Modal";
import ConfirmDialog from "./ConfirmDialog";
import ErrorText from "./ErrorText";

interface EditableHeaderProps {
  backTo: string;
  name: string;
  description: string;
  /** Kind of thing, for dialog copy ("exercise", "workout"). */
  noun: string;
  deleteMessage?: string;
  maxNameLength: number;
  onSave: (values: { name: string; description: string }) => Promise<unknown>;
  onDelete: () => Promise<unknown>;
}

/** Back link, delete button and a title/description card with inline edit dialogs. */
export default function EditableHeader({
  backTo,
  name,
  description,
  noun,
  deleteMessage,
  maxNameLength,
  onSave,
  onDelete,
}: EditableHeaderProps) {
  const [editing, setEditing] = useState<"name" | "description" | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [draftName, setDraftName] = useState(name);
  const [draftDescription, setDraftDescription] = useState(description);
  const [error, setError] = useState<unknown>(null);
  const [pending, setPending] = useState(false);

  const open = (field: "name" | "description") => {
    setDraftName(name);
    setDraftDescription(description);
    setError(null);
    setEditing(field);
  };

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);
    try {
      await onSave({ name: draftName.trim(), description: draftDescription.trim() });
      setEditing(null);
    } catch (err) {
      setError(err);
    } finally {
      setPending(false);
    }
  }

  async function remove() {
    setPending(true);
    setError(null);
    try {
      await onDelete();
    } catch (err) {
      setError(err);
      setPending(false);
    }
  }

  return (
    <div className="flex flex-col gap-4 text-white">
      <div className="flex items-center justify-between">
        <Link className="flex items-center gap-2 text-sm text-gray-400 transition hover:text-white" to={backTo}>
          <FaLongArrowAltLeft /> Back
        </Link>
        <button className="btn-ghost text-red-400" onClick={() => setConfirmDelete(true)}>
          <FaRegTrashAlt /> Delete
        </button>
      </div>

      <div className="card">
        <div className="flex items-start justify-between gap-3">
          <h1 className="page-header break-words">{name}</h1>
          <button aria-label="Edit name" className="mt-2 text-gray-400 transition hover:text-white" onClick={() => open("name")}>
            <FaPen size={16} />
          </button>
        </div>
        <div className="mt-2 flex items-start justify-between gap-3">
          <p className="whitespace-pre-wrap break-words text-gray-400">{description}</p>
          <button
            aria-label="Edit description"
            className="shrink-0 text-gray-400 transition hover:text-white"
            onClick={() => open("description")}
          >
            <FaPen size={14} />
          </button>
        </div>
      </div>

      <ConfirmDialog
        open={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        onConfirm={remove}
        title={`Delete ${noun} “${name}”?`}
        message={deleteMessage}
        pending={pending}
        error={confirmDelete ? error : null}
      />

      <Modal open={editing !== null} onClose={() => setEditing(null)}>
        <form autoComplete="off" onSubmit={save} className="flex flex-col gap-3">
          <h3 className="text-lg font-bold">Edit {editing}</h3>
          {editing === "name" ? (
            <input
              maxLength={maxNameLength}
              required
              value={draftName}
              onChange={(e) => setDraftName(e.target.value)}
              aria-label={`${noun} name`}
              className="input-field"
            />
          ) : (
            <textarea
              required
              value={draftDescription}
              onChange={(e) => setDraftDescription(e.target.value)}
              aria-label={`${noun} description`}
              className="input-field h-64"
            />
          )}
          <ErrorText error={error} />
          <button className="btn-primary" type="submit" disabled={pending}>
            {pending ? "Saving…" : "Save"}
          </button>
        </form>
      </Modal>
    </div>
  );
}
