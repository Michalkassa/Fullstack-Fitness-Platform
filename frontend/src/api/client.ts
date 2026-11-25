/**
 * Thin fetch wrapper: adds the base URL and bearer token, parses JSON, and turns
 * non-2xx responses into ApiError (reading RFC 7807 problem details if present).
 */

const BASE_URL = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");
const TOKEN_KEY = "lockedin.token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

let unauthorizedHandler: (() => void) | null = null;

/** Called whenever an authenticated request comes back 401 (expired/invalid token). */
export function onUnauthorized(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface ProblemBody {
  detail?: string;
  title?: string;
  message?: string;
  errors?: Record<string, string>;
}

async function toApiError(res: Response): Promise<ApiError> {
  let body: ProblemBody = {};
  try {
    body = (await res.json()) as ProblemBody;
  } catch {
    // not JSON
  }
  const fieldErrors = body.errors ?? {};
  const message =
    body.detail ||
    body.message ||
    Object.values(fieldErrors)[0] ||
    body.title ||
    defaultMessage(res.status);
  return new ApiError(res.status, message, fieldErrors);
}

function defaultMessage(status: number): string {
  if (status === 401) return "Please sign in again.";
  if (status === 403) return "You don't have access to that.";
  if (status === 404) return "Not found.";
  if (status === 501) return "This endpoint isn't implemented yet.";
  if (status >= 500) return "Something went wrong on the server.";
  return "Request failed.";
}

type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

async function rawRequest(method: Method, path: string, body?: unknown): Promise<Response> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "Can't reach the server. Is the backend running?");
  }

  if (!res.ok) {
    if (res.status === 401 && token) unauthorizedHandler?.();
    throw await toApiError(res);
  }
  return res;
}

export async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const res = await rawRequest(method, path, body);
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/** Fetch a file (CSV/PDF) with auth and return it as a Blob plus its filename. */
export async function requestBlob(path: string): Promise<{ blob: Blob; filename: string | null }> {
  const res = await rawRequest("GET", path);
  const disposition = res.headers.get("Content-Disposition") ?? "";
  const match = /filename="?([^";]+)"?/i.exec(disposition);
  return { blob: await res.blob(), filename: match?.[1] ?? null };
}

export const http = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
  patch: <T>(path: string, body?: unknown) => request<T>("PATCH", path, body),
  delete: <T = void>(path: string) => request<T>("DELETE", path),
};
