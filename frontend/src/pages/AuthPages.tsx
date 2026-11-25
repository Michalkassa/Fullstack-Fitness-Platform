import { useState, type FormEvent, type ReactNode } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import ErrorText from "@/components/ErrorText";
import { validateLogin, validateRegister } from "@/lib/validation";

function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="flex min-h-screen flex-col items-center justify-center px-6 py-8">
      <Link to="/" className="mb-6 flex items-center gap-2 text-2xl font-semibold text-white">
        <img className="h-8 w-8" src="/logo.png" alt="" />
        LockedIn
      </Link>
      <div className="card w-full sm:max-w-md">
        <h1 className="mb-6 text-xl font-bold tracking-tight text-white md:text-2xl">{title}</h1>
        {children}
      </div>
    </section>
  );
}

function useAfterAuthRedirect() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/dashboard";
  return () => navigate(from, { replace: true });
}

export function LoginPage() {
  const { login } = useAuth();
  const redirect = useAfterAuthRedirect();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [pending, setPending] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const problem = validateLogin({ email, password });
    if (problem) return setError(problem);
    setPending(true);
    setError(null);
    try {
      await login({ email: email.trim(), password });
      redirect();
    } catch (err) {
      setError(err);
      setPending(false);
    }
  }

  return (
    <AuthShell title="Sign in to your account">
      <form className="space-y-5" onSubmit={submit} noValidate>
        <div>
          <label htmlFor="email" className="mb-2 block text-sm font-medium text-white">Your email</label>
          <input id="email" type="email" autoComplete="email" placeholder="name@company.com" className="input-field"
            value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label htmlFor="password" className="mb-2 block text-sm font-medium text-white">Password</label>
          <input id="password" type="password" autoComplete="current-password" placeholder="••••••••" className="input-field"
            value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <ErrorText error={error} />
        <button type="submit" className="btn-primary w-full" disabled={pending}>
          {pending ? "Signing in…" : "Sign in"}
        </button>
        <p className="text-sm text-gray-400">
          Don’t have an account?{" "}
          <Link to="/register" className="font-medium text-white hover:underline">Sign up</Link>
        </p>
      </form>
    </AuthShell>
  );
}

export function RegisterPage() {
  const { register } = useAuth();
  const redirect = useAfterAuthRedirect();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [pending, setPending] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const problem = validateRegister({ email, password, confirmPassword });
    if (problem) return setError(problem);
    setPending(true);
    setError(null);
    try {
      await register({ email: email.trim(), password });
      redirect();
    } catch (err) {
      setError(err);
      setPending(false);
    }
  }

  return (
    <AuthShell title="Create an account">
      <form className="space-y-5" onSubmit={submit} noValidate>
        <div>
          <label htmlFor="email" className="mb-2 block text-sm font-medium text-white">Your email</label>
          <input id="email" type="email" autoComplete="email" placeholder="name@company.com" className="input-field"
            value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label htmlFor="password" className="mb-2 block text-sm font-medium text-white">Password</label>
          <input id="password" type="password" autoComplete="new-password" placeholder="••••••••" className="input-field"
            value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <div>
          <label htmlFor="confirm-password" className="mb-2 block text-sm font-medium text-white">Confirm password</label>
          <input id="confirm-password" type="password" autoComplete="new-password" placeholder="••••••••" className="input-field"
            value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
        </div>
        <ErrorText error={error} />
        <button type="submit" className="btn-primary w-full" disabled={pending}>
          {pending ? "Creating account…" : "Create account"}
        </button>
        <p className="text-sm text-gray-400">
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-white hover:underline">Log in</Link>
        </p>
      </form>
    </AuthShell>
  );
}
