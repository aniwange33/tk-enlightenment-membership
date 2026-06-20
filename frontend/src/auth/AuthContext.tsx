import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { authApi, tokenStore } from "../api/client";
import type { Role } from "../api/types";
import { decodeJwt, isExpired } from "./jwt";

interface AuthState {
  token: string | null;
  role: Role | null;
  memberId: string | null;
  email: string | null;
  mustChangePassword: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<{ mustChangePassword: boolean }>;
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

function initialToken(): string | null {
  const t = tokenStore.get();
  if (t && !isExpired(decodeJwt(t))) return t;
  if (t) tokenStore.clear();
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(initialToken);

  const apply = useCallback((t: string) => {
    tokenStore.set(t);
    setToken(t);
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await authApi.login(email, password);
      apply(res.token);
      return { mustChangePassword: res.mustChangePassword };
    },
    [apply],
  );

  const changePassword = useCallback(
    async (currentPassword: string, newPassword: string) => {
      const res = await authApi.changePassword(currentPassword, newPassword);
      apply(res.token); // fresh token without the must-change flag
    },
    [apply],
  );

  const logout = useCallback(() => {
    tokenStore.clear();
    setToken(null);
  }, []);

  const value = useMemo<AuthState>(() => {
    const claims = token ? decodeJwt(token) : null;
    return {
      token,
      role: claims?.role ?? null,
      memberId: claims?.memberId ?? null,
      email: claims?.email ?? null,
      mustChangePassword: claims?.mustChangePassword ?? false,
      isAuthenticated: !!claims && !isExpired(claims),
      login,
      changePassword,
      logout,
    };
  }, [token, login, changePassword, logout]);

  return <AuthContext value={value}>{children}</AuthContext>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
