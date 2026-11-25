import { Link } from "react-router";
import { useAuth } from "@/auth/AuthContext";

export default function PublicNavbar() {
  const { isAuthenticated } = useAuth();
  return (
    <header className="relative z-10 flex items-center justify-between px-6 py-5 md:px-12">
      <Link className="flex items-center gap-2 text-xl font-semibold tracking-tight text-white md:text-2xl" to="/">
        <img src="/logo.png" alt="" className="h-7 w-7" />
        LockedIn
      </Link>
      <nav className="flex items-center gap-3">
        {isAuthenticated ? (
          <Link to="/dashboard" className="btn-primary">
            Go to dashboard
          </Link>
        ) : (
          <>
            <Link to="/login" className="btn-ghost">
              Sign in
            </Link>
            <Link to="/register" className="btn-primary">
              Sign up
            </Link>
          </>
        )}
      </nav>
    </header>
  );
}
