import { ApiError } from "@/api/client";

export function errorMessage(error: unknown): string {
  if (!error) return "";
  if (error instanceof ApiError || error instanceof Error) return error.message;
  return String(error);
}

/** Inline red error text; renders nothing when there's no error. */
export default function ErrorText({ error }: { error: unknown }) {
  const msg = typeof error === "string" ? error : errorMessage(error);
  if (!msg) return null;
  return (
    <p role="alert" className="text-sm text-red-400">
      {msg}
    </p>
  );
}
