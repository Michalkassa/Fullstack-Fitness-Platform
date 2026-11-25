import Modal from "./Modal";
import ErrorText from "./ErrorText";

interface ConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message?: string;
  confirmLabel?: string;
  pending?: boolean;
  error?: unknown;
}

export default function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  message = "This can’t be undone.",
  confirmLabel = "Delete",
  pending,
  error,
}: ConfirmDialogProps) {
  return (
    <Modal open={open} onClose={onClose}>
      <div className="flex flex-col gap-4">
        <h3 className="pr-6 text-lg font-bold">{title}</h3>
        <p className="text-gray-400">{message}</p>
        <ErrorText error={error} />
        <div className="flex gap-3">
          <button className="btn-danger" onClick={onConfirm} disabled={pending} type="button">
            {pending ? "Working…" : confirmLabel}
          </button>
          <button className="btn-ghost" onClick={onClose} type="button">
            Cancel
          </button>
        </div>
      </div>
    </Modal>
  );
}
