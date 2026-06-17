import type {
  AnnouncementResponse,
  DuesRecord,
  LoginResponse,
  Member,
  MemberStatus,
  Page,
  RecipientGroup,
  RegisterMemberInput,
} from "./types";

const TOKEN_KEY = "tec.token";

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/** Surfaces the backend ProblemDetail (status + detail + optional field errors). */
export class ApiError extends Error {
  status: number;
  errors?: string[];
  constructor(status: number, detail: string, errors?: string[]) {
    super(detail);
    this.status = status;
    this.errors = errors;
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const token = tokenStore.get();
  const res = await fetch(`/api${path}`, {
    method,
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as T;
  }

  const isJson = res.headers.get("content-type")?.includes("json");
  const payload = isJson ? await res.json().catch(() => null) : await res.text();

  if (!res.ok) {
    const detail =
      (payload && typeof payload === "object" && "detail" in payload
        ? (payload as { detail?: string }).detail
        : null) || `Request failed (${res.status})`;
    const errors =
      payload && typeof payload === "object" && "errors" in payload
        ? (payload as { errors?: string[] }).errors
        : undefined;
    throw new ApiError(res.status, detail, errors);
  }
  return payload as T;
}

/* ── Auth (public except change-password) ─────────────────────────────────── */
export const authApi = {
  login: (email: string, password: string) =>
    request<LoginResponse>("POST", "/auth/login", { email, password }),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<LoginResponse>("POST", "/auth/change-password", { currentPassword, newPassword }),
  forgotPassword: (email: string) =>
    request<{ message: string }>("POST", "/auth/forgot-password", { email }),
  resetPassword: (token: string, newPassword: string) =>
    request<{ message: string }>("POST", "/auth/reset-password", { token, newPassword }),
};

/* ── Member self-service ──────────────────────────────────────────────────── */
export const meApi = {
  get: () => request<Member>("GET", "/me"),
  update: (phone: string, address: string) =>
    request<Member>("PUT", "/me", { phone, address }),
  dues: () => request<DuesRecord[]>("GET", "/me/dues"),
};

/* ── Admin: members + dues ────────────────────────────────────────────────── */
export const membersApi = {
  list: (params: { query?: string; status?: MemberStatus | ""; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params.query) q.set("query", params.query);
    if (params.status) q.set("status", params.status);
    q.set("page", String(params.page ?? 0));
    q.set("size", String(params.size ?? 8));
    return request<Page<Member>>("GET", `/members?${q.toString()}`);
  },
  register: (input: RegisterMemberInput) => request<Member>("POST", "/members", input),
  get: (id: string) => request<Member>("GET", `/members/${id}`),
  changeStatus: (id: string, status: MemberStatus) =>
    request<Member>("PATCH", `/members/${id}/status`, { status }),
  dues: (id: string) => request<DuesRecord[]>("GET", `/members/${id}/dues`),
  payDues: (id: string, year: number) =>
    request<DuesRecord>("POST", `/members/${id}/dues/${year}/pay`),
};

/* ── Announcements ────────────────────────────────────────────────────────── */
export const announcementsApi = {
  send: (subject: string, body: string, recipientGroup: RecipientGroup) =>
    request<AnnouncementResponse>("POST", "/announcements", { subject, body, recipientGroup }),
};
