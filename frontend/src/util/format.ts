import type { Member } from "../api/types";

const LONG = ["January","February","March","April","May","June","July","August","September","October","November","December"];
const SHORT = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

export function longDate(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso + "T00:00:00");
  return `${String(d.getDate()).padStart(2, "0")} ${LONG[d.getMonth()]} ${d.getFullYear()}`;
}

export function shortDate(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso + "T00:00:00");
  return `${String(d.getDate()).padStart(2, "0")} ${SHORT[d.getMonth()]} ${d.getFullYear()}`;
}

export const titleCase = (s: string): string => s.charAt(0) + s.slice(1).toLowerCase();
export const fullName = (m: Pick<Member, "firstName" | "lastName">): string => `${m.firstName} ${m.lastName}`;
export const yearOf = (iso: string): string => iso.slice(0, 4);
export const initialsOf = (m: Pick<Member, "firstName" | "lastName">): string =>
  (m.firstName[0] + m.lastName[0]).toUpperCase();
