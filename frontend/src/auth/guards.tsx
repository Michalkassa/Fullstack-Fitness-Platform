import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "./AuthContext";

/** Renders child routes only for signed-in users; otherwise sends them to /login. */
export function RequireAuth() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}

/** Login/register pages: signed-in users go straight to the dashboard. */
export function GuestOnly() {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}
