import { useCallback, useEffect, useState, type FormEvent } from "react";
import TopBar from "../components/TopBar";
import StatusBadge from "../components/StatusBadge";
import { ApiError, membersApi, announcementsApi } from "../api/client";
import type { DuesRecord, Member, MemberStatus, RecipientGroup, RegisterMemberInput } from "../api/types";
import { fullName, shortDate, titleCase, yearOf } from "../util/format";

const STATUS_TRANSITIONS: Record<MemberStatus, MemberStatus[]> = {
  ACTIVE: ["INACTIVE", "SUSPENDED", "TERMINATED"],
  INACTIVE: ["ACTIVE", "SUSPENDED", "TERMINATED"],
  SUSPENDED: ["ACTIVE", "TERMINATED"],
  TERMINATED: [],
};
const isEmail = (v: string) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v.trim());

export default function AdminConsole() {
  const [section, setSection] = useState<"members" | "announcements">("members");
  const [toast, setToast] = useState<string | null>(null);
  const showToast = useCallback((t: string) => {
    setToast(t);
    window.setTimeout(() => setToast(null), 2800);
  }, []);

  /* roster */
  const [search, setSearch] = useState("");
  const [debounced, setDebounced] = useState("");
  const [status, setStatus] = useState<MemberStatus | "">("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<Member[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [rosterErr, setRosterErr] = useState<string | null>(null);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 300);
    return () => clearTimeout(t);
  }, [search]);

  const loadRoster = useCallback(async () => {
    setLoading(true);
    setRosterErr(null);
    try {
      const p = await membersApi.list({ query: debounced, status, page, size: 8 });
      setItems(p.content);
      setTotalPages(p.totalPages || 1);
      setTotalElements(p.totalElements);
    } catch (e) {
      setRosterErr((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, [debounced, status, page]);

  useEffect(() => {
    loadRoster();
  }, [loadRoster]);

  /* drawer */
  const [active, setActive] = useState<Member | null>(null);
  const [activeDues, setActiveDues] = useState<DuesRecord[]>([]);
  const [statusChoice, setStatusChoice] = useState<MemberStatus | "">("");

  const openDrawer = useCallback(async (m: Member) => {
    setActive(m);
    setStatusChoice(m.status);
    setActiveDues([]);
    try {
      const d = await membersApi.dues(m.id);
      setActiveDues([...d].sort((a, b) => b.year - a.year));
    } catch {
      /* dues optional */
    }
  }, []);
  const closeDrawer = () => setActive(null);

  async function applyStatus() {
    if (!active || !statusChoice || statusChoice === active.status) {
      showToast("No change — status is already " + titleCase(active!.status) + ".");
      return;
    }
    try {
      const updated = await membersApi.changeStatus(active.id, statusChoice);
      setActive(updated);
      showToast(`${fullName(updated)} is now ${titleCase(updated.status)}.`);
      loadRoster();
    } catch (e) {
      showToast((e as Error).message);
    }
  }

  async function payDues(year: number) {
    if (!active) return;
    try {
      await membersApi.payDues(active.id, year);
      const [m, d] = await Promise.all([membersApi.get(active.id), membersApi.dues(active.id)]);
      setActive(m);
      setActiveDues([...d].sort((a, b) => b.year - a.year));
      showToast(`Dues ${year} recorded for ${fullName(m)}.`);
      loadRoster();
    } catch (e) {
      showToast((e as Error).message);
    }
  }

  /* register modal */
  const [modal, setModal] = useState(false);

  /* announcement counts */
  const [counts, setCounts] = useState<{ active: number; all: number } | null>(null);
  useEffect(() => {
    if (section !== "announcements") return;
    (async () => {
      try {
        const [activeP, allP, termP] = await Promise.all([
          membersApi.list({ status: "ACTIVE", page: 0, size: 1 }),
          membersApi.list({ page: 0, size: 1 }),
          membersApi.list({ status: "TERMINATED", page: 0, size: 1 }),
        ]);
        setCounts({ active: activeP.totalElements, all: allP.totalElements - termP.totalElements });
      } catch {
        setCounts(null);
      }
    })();
  }, [section]);

  return (
    <div className="app admin">
      <div className="field" aria-hidden="true"><div className="halo halo--admin" /><div className="grain" /></div>
      <TopBar section="Admin" name="Secretary" sub="officer@taraku.org" initials="S" />

      <div className="console">
        <aside className="side">
          <nav className="nav">
            <button className={`nav__item${section === "members" ? " is-active" : ""}`} onClick={() => setSection("members")}>
              <span className="nav__ico">❖</span>Members
            </button>
            <button className={`nav__item${section === "announcements" ? " is-active" : ""}`} onClick={() => setSection("announcements")}>
              <span className="nav__ico">✸</span>Announcements
            </button>
          </nav>
          <div className="side__foot">
            <span className="side__stat">{totalElements}</span>
            <span className="side__statlbl">on the roster</span>
          </div>
        </aside>

        {section === "members" ? (
          <main className="main">
            <div className="main__head">
              <div><p className="eyebrow">Roster</p><h1 className="main__title">Members</h1></div>
              <button className="btn btn--sm" onClick={() => setModal(true)}><span>＋ Register member</span><i className="btn__spark" /></button>
            </div>

            <div className="toolbar">
              <label className="search">
                <span className="search__ico" aria-hidden="true">⌕</span>
                <input type="search" placeholder="Search name, email, or number…" value={search}
                       onChange={(e) => { setSearch(e.target.value); setPage(0); }} aria-label="Search members" />
              </label>
              <select className="select" value={status} aria-label="Filter by status"
                      onChange={(e) => { setStatus(e.target.value as MemberStatus | ""); setPage(0); }}>
                <option value="">All statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="TERMINATED">Terminated</option>
              </select>
            </div>

            <div className="tbl-wrap">
              <table className="tbl">
                <thead><tr><th>Member</th><th>No.</th><th>Status</th><th>Joined</th><th></th></tr></thead>
                <tbody>
                  {items.map((m) => (
                    <tr key={m.id} onClick={() => openDrawer(m)}>
                      <td><span className="cell-name"><b>{fullName(m)}</b><span>{m.email}</span></span></td>
                      <td className="cell-no">{m.membershipNumber}</td>
                      <td><StatusBadge status={m.status} /></td>
                      <td className="cell-joined">{yearOf(m.joinDate)}</td>
                      <td className="cell-view">View →</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!loading && items.length === 0 && <p className="empty">{rosterErr ?? "No members match your search."}</p>}
              {loading && <p className="empty">Loading…</p>}
            </div>

            <div className="pager">
              <button className="btn btn--ghost btn--sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}><span>← Prev</span></button>
              <span className="pager__info">{totalElements ? `Page ${page + 1} of ${totalPages} · ${totalElements} member${totalElements > 1 ? "s" : ""}` : "No results"}</span>
              <button className="btn btn--ghost btn--sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}><span>Next →</span></button>
            </div>
          </main>
        ) : (
          <AnnouncementForm counts={counts} onSent={(n) => showToast(`Announcement queued for ${n} member${n === 1 ? "" : "s"}.`)} />
        )}
      </div>

      {active && (
        <>
          <div className="scrim" onClick={closeDrawer} />
          <aside className="drawer" aria-label="Member detail">
            <button className="drawer__close" onClick={closeDrawer} aria-label="Close">✕</button>
            <div className="drawer__head">
              <StatusBadge status={active.status} />
              <h2 className="drawer__name">{fullName(active)}</h2>
              <span className="drawer__no">{active.membershipNumber}</span>
            </div>
            <dl className="drawer__facts">
              <div><dt>Email</dt><dd>{active.email}</dd></div>
              <div><dt>Phone</dt><dd>{active.phone ?? "—"}</dd></div>
              <div><dt>Joined</dt><dd>{yearOf(active.joinDate)}</dd></div>
            </dl>

            <div className="drawer__sec">
              <h3 className="drawer__h3">Status</h3>
              <div className="statusrow">
                <select className="select" value={statusChoice} disabled={STATUS_TRANSITIONS[active.status].length === 0}
                        onChange={(e) => setStatusChoice(e.target.value as MemberStatus)}>
                  {[active.status, ...STATUS_TRANSITIONS[active.status]].map((s) => (
                    <option key={s} value={s}>{titleCase(s)}</option>
                  ))}
                </select>
                <button className="btn btn--sm" disabled={STATUS_TRANSITIONS[active.status].length === 0} onClick={applyStatus}><span>Update</span></button>
              </div>
              {STATUS_TRANSITIONS[active.status].length === 0 && <p className="note">Terminated is a terminal state and can't be changed.</p>}
            </div>

            <div className="drawer__sec">
              <h3 className="drawer__h3">Dues</h3>
              <table className="dues">
                <tbody>
                  {activeDues.map((d) => (
                    <tr key={d.year}>
                      <td>{d.year}</td>
                      {d.paid ? (
                        <>
                          <td><span className="pill pill--paid">Paid</span></td>
                          <td className={d.paidDate ? "" : "muted"}>{d.paidDate ? shortDate(d.paidDate) : "—"}</td>
                        </>
                      ) : (
                        <>
                          <td><span className="pill pill--unpaid">Unpaid</span></td>
                          <td><button className="btn btn--sm" onClick={() => payDues(d.year)}><span>Mark paid</span></button></td>
                        </>
                      )}
                    </tr>
                  ))}
                  {activeDues.length === 0 && <tr><td className="muted" colSpan={3}>No dues records.</td></tr>}
                </tbody>
              </table>
            </div>
          </aside>
        </>
      )}

      {modal && <RegisterModal onClose={() => setModal(false)} onRegistered={(m) => {
        setModal(false);
        setSearch(""); setStatus(""); setPage(0);
        loadRoster();
        showToast(`${fullName(m)} registered as ${m.membershipNumber}.`);
      }} />}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}

/* ── Register modal ──────────────────────────────────────────────────────── */
function RegisterModal({ onClose, onRegistered }: { onClose: () => void; onRegistered: (m: Member) => void }) {
  const [form, setForm] = useState<RegisterMemberInput>({ firstName: "", lastName: "", dateOfBirth: "", email: "", phone: "", address: "" });
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const set = (k: keyof RegisterMemberInput) => (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.firstName.trim() || !form.lastName.trim() || !form.dateOfBirth) return setError("First name, last name and date of birth are required.");
    if (!isEmail(form.email)) return setError("Enter a valid email address.");
    setPending(true);
    try {
      const m = await membersApi.register({ ...form, phone: form.phone || undefined, address: form.address || undefined });
      onRegistered(m);
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "A member with that email already exists." : (err as Error).message);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="modal" role="dialog" aria-modal="true" aria-label="Register member">
      <div className="modal__backdrop" onClick={onClose} />
      <form className="modal__card" onSubmit={onSubmit} noValidate>
        <button type="button" className="drawer__close" onClick={onClose} aria-label="Close">✕</button>
        <p className="eyebrow">New enrolment</p>
        <h2 className="heading">Register a member</h2>
        {error && <div className="msg msg--error">{error}</div>}
        <div className="modal__grid">
          <label className="field-row"><span className="label">First name</span><input value={form.firstName} onChange={set("firstName")} /></label>
          <label className="field-row"><span className="label">Last name</span><input value={form.lastName} onChange={set("lastName")} /></label>
          <label className="field-row"><span className="label">Date of birth</span><input type="date" value={form.dateOfBirth} onChange={set("dateOfBirth")} /></label>
          <label className="field-row"><span className="label">Email</span><input type="email" value={form.email} onChange={set("email")} /></label>
          <label className="field-row"><span className="label">Phone</span><input type="tel" value={form.phone} onChange={set("phone")} /></label>
          <label className="field-row"><span className="label">Address</span><input value={form.address} onChange={set("address")} /></label>
        </div>
        <p className="note">A membership number is generated automatically; a welcome email with a temporary password is sent.</p>
        <div className="modal__actions">
          <button className="btn" type="submit" disabled={pending}><span>{pending ? "Registering…" : "Register member"}</span><i className="btn__spark" /></button>
          <button className="btn btn--ghost" type="button" onClick={onClose}><span>Cancel</span></button>
        </div>
      </form>
    </div>
  );
}

/* ── Announcements ───────────────────────────────────────────────────────── */
function AnnouncementForm({ counts, onSent }: { counts: { active: number; all: number } | null; onSent: (n: number) => void }) {
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [group, setGroup] = useState<RecipientGroup>("ACTIVE_MEMBERS");
  const [msg, setMsg] = useState<{ type: "error" | "success"; text: string } | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setMsg(null);
    if (!subject.trim() || !body.trim()) return setMsg({ type: "error", text: "Subject and message are required." });
    setPending(true);
    try {
      const res = await announcementsApi.send(subject.trim(), body.trim(), group);
      setMsg({ type: "success", text: `Announcement sent to ${res.recipientCount} member${res.recipientCount === 1 ? "" : "s"}.` });
      setSubject(""); setBody("");
      onSent(res.recipientCount);
    } catch (err) {
      setMsg({ type: "error", text: (err as Error).message });
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="main">
      <div className="main__head"><div><p className="eyebrow">Communications</p><h1 className="main__title">Send an announcement</h1></div></div>
      <form className="compose" onSubmit={onSubmit} noValidate>
        {msg && <div className={`msg msg--${msg.type}`}>{msg.text}</div>}
        <label className="field-row">
          <span className="label">Subject</span>
          <input value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="e.g. Annual General Meeting — 12 July" />
        </label>
        <label className="field-row">
          <span className="label">Message</span>
          <textarea rows={7} value={body} onChange={(e) => setBody(e.target.value)} placeholder="Write your announcement…" />
        </label>
        <fieldset className="seg">
          <legend className="label">Recipients</legend>
          <label className="seg__opt"><input type="radio" name="group" checked={group === "ACTIVE_MEMBERS"} onChange={() => setGroup("ACTIVE_MEMBERS")} />
            <span>Active members <em>{counts ? `(${counts.active})` : ""}</em></span></label>
          <label className="seg__opt"><input type="radio" name="group" checked={group === "ALL_MEMBERS"} onChange={() => setGroup("ALL_MEMBERS")} />
            <span>All members <em>{counts ? `(${counts.all})` : ""}</em></span></label>
        </fieldset>
        <p className="note">“All members” excludes those who have left the club (terminated).</p>
        <button className="btn" type="submit" disabled={pending}><span>{pending ? "Sending…" : "Send announcement"}</span><i className="btn__spark" /></button>
      </form>
    </main>
  );
}
