import type { Role } from "../api/types";

export interface JwtClaims {
  sub: string; // accountId
  email: string;
  role: Role;
  memberId: string | null;
  mustChangePassword: boolean;
  exp: number; // seconds
}

/** Decode the JWT payload (no verification — the server verifies on every request). */
export function decodeJwt(token: string): JwtClaims | null {
  try {
    const payload = token.split(".")[1];
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json) as JwtClaims;
  } catch {
    return null;
  }
}

export const isExpired = (claims: JwtClaims | null): boolean =>
  !claims || claims.exp * 1000 <= Date.now();
