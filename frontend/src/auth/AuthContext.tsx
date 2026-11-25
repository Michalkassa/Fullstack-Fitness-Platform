import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getToken, onUnauthorized, setToken } from "@/api/client";
import { authApi, usersApi } from "@/api/endpoints";
import { keys } from "@/api/queries";
import type { LoginRequest, RegisterRequest, User } from "@/api/types";

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  /** True while we hold a token but haven't loaded the user yet. */
  isLoading: boolean;
  login: (body: LoginRequest) => Promise<void>;
  register: (body: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient();
  const [token, setTokenState] = useState<string | null>(() => getToken());

  const saveToken = useCallback((value: string | null) => {
    setToken(value);
    setTokenState(value);
  }, []);

  const logout = useCallback(() => {
    saveToken(null);
    qc.clear();
  }, [qc, saveToken]);

  // Any 401 from an authenticated request means the token is no good any more.
  useEffect(() => {
    onUnauthorized(logout);
    return () => onUnauthorized(null);
  }, [logout]);

  const me = useQuery({
    queryKey: keys.me,
    queryFn: usersApi.me,
    enabled: !!token,
    retry: false,
  });

  const login = useCallback(
    async (body: LoginRequest) => {
      const res = await authApi.login(body);
      saveToken(res.token);
      qc.setQueryData(keys.me, res.user);
    },
    [qc, saveToken],
  );

  const register = useCallback(
    async (body: RegisterRequest) => {
      const res = await authApi.register(body);
      saveToken(res.token);
      qc.setQueryData(keys.me, res.user);
    },
    [qc, saveToken],
  );

  const value = useMemo<AuthState>(
    () => ({
      user: me.data ?? null,
      isAuthenticated: !!token,
      isLoading: !!token && me.isPending,
      login,
      register,
      logout,
    }),
    [token, me.data, me.isPending, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
