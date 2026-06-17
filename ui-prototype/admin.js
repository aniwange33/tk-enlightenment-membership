/* ============================================================================
   Admin console — mock data + interactions, mirroring the real contracts:
     GET   /api/members?query=&status=&page    -> Page<MemberResponse>
     POST  /api/members                         -> register (201)
     PATCH /api/members/{id}/status             -> { status }
     GET   /api/members/{id}/dues               -> [{ year, paid, paidDate }]
     POST  /api/members/{id}/dues/{year}/pay    -> record payment (reactivates if INACTIVE)
     POST  /api/announcements                   -> { recipientCount }
   Swap the in-memory ops for fetch() with the Bearer token to wire the API.
   ========================================================================== */

const $  = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

const YEAR = 2026;
const TODAY = "2026-06-17";
const PAGE_SIZE = 8;
const STATUS_TRANSITIONS = {
  ACTIVE:     ["INACTIVE", "SUSPENDED", "TERMINATED"],
  INACTIVE:   ["ACTIVE", "SUSPENDED", "TERMINATED"],
  SUSPENDED:  ["ACTIVE", "TERMINATED"],
  TERMINATED: [],
};

/* ── Mock roster ─────────────────────────────────────────────────────────── */
const seed = [
  ["Amara","Okonkwo","2021",14,"ACTIVE",   {2026:1,2025:1,2024:1}],
  ["Tunde","Bakare","2019",3,"ACTIVE",      {2026:1,2025:1,2024:1,2023:1}],
  ["Ngozi","Eze","2022",41,"INACTIVE",      {2026:0,2025:1,2024:1}],
  ["Sefu","Abara","2020",27,"ACTIVE",       {2026:1,2025:1}],
  ["Halima","Sani","2023",8,"ACTIVE",       {2026:0,2025:1}],
  ["Chidi","Nwosu","2018",2,"SUSPENDED",    {2026:0,2025:0,2024:1}],
  ["Zainab","Bello","2024",19,"ACTIVE",     {2026:1}],
  ["Emeka","Obi","2021",33,"TERMINATED",    {2025:1,2024:1,2023:0}],
  ["Fatima","Yusuf","2022",12,"ACTIVE",     {2026:1,2025:1}],
  ["Kwame","Mensah","2020",5,"INACTIVE",    {2026:0,2025:0}],
  ["Adaeze","Ibe","2023",22,"ACTIVE",       {2026:1,2025:1}],
  ["Bayo","Adeyemi","2019",7,"ACTIVE",      {2026:1,2025:1,2024:1}],
  ["Rukayat","Lawal","2024",30,"ACTIVE",    {2026:0}],
  ["Ifeanyi","Okafor","2022",17,"SUSPENDED",{2026:0,2025:1}],
  ["Maryam","Garba","2021",9,"ACTIVE",      {2026:1,2025:1,2024:1}],
];
const PAID_DATES = { 2026:"2026-03-09", 2025:"2025-03-12", 2024:"2024-02-27", 2023:"2023-04-11" };

let members = seed.map(([first, last, yr, n, status, dues]) => ({
  id: `m_${first}_${n}`.toLowerCase(),
  membershipNumber: `TEC-${yr}-${String(n).padStart(3, "0")}`,
  firstName: first, lastName: last,
  email: `${first}.${last}`.toLowerCase() + "@taraku.org",
  phone: "+234 80" + (1000000 + n * 137).toString().slice(0, 7),
  joinYear: yr,
  status,
  dues: Object.fromEntries(Object.entries(dues).map(([y, p]) => [y, { paid: !!p, paidDate: p ? PAID_DATES[y] : null }])),
}));

const state = { search: "", status: "", page: 0 };

/* ── Helpers ─────────────────────────────────────────────────────────────── */
const MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
const shortDate = (iso) => { const d = new Date(iso + "T00:00:00"); return `${String(d.getDate()).padStart(2,"0")} ${MONTHS[d.getMonth()]} ${d.getFullYear()}`; };
const titleCase = (s) => s.charAt(0) + s.slice(1).toLowerCase();
const fullName = (m) => `${m.firstName} ${m.lastName}`;
const badge = (s) => `<span class="badge badge--${s.toLowerCase()}">${titleCase(s)}</span>`;

function toast(text) {
  const t = $("[data-toast]"); t.textContent = text; t.hidden = false;
  clearTimeout(toast._t); toast._t = setTimeout(() => (t.hidden = true), 2800);
}

function filtered() {
  const q = state.search.trim().toLowerCase();
  return members.filter((m) => {
    if (state.status && m.status !== state.status) return false;
    if (!q) return true;
    return fullName(m).toLowerCase().includes(q)
      || m.email.toLowerCase().includes(q)
      || m.membershipNumber.toLowerCase().includes(q);
  });
}

/* ── Render: roster table ────────────────────────────────────────────────── */
function renderTable() {
  const rows = filtered();
  const pages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  state.page = Math.min(state.page, pages - 1);
  const slice = rows.slice(state.page * PAGE_SIZE, state.page * PAGE_SIZE + PAGE_SIZE);

  $("[data-year-head]").textContent = `Dues ${YEAR}`;
  $("[data-rows]").innerHTML = slice.map((m) => {
    const d = m.dues[YEAR];
    const pill = d
      ? `<span class="pill pill--${d.paid ? "paid" : "unpaid"}">${d.paid ? "Paid" : "Unpaid"}</span>`
      : `<span class="pill pill--unpaid">None</span>`;
    return `<tr data-id="${m.id}">
      <td><span class="cell-name"><b>${fullName(m)}</b><span>${m.email}</span></span></td>
      <td class="cell-no">${m.membershipNumber}</td>
      <td>${badge(m.status)}</td>
      <td>${pill}</td>
      <td class="cell-joined">${m.joinYear}</td>
      <td class="cell-view">View →</td>
    </tr>`;
  }).join("");

  $("[data-empty]").hidden = rows.length !== 0;
  $("[data-page-info]").textContent = rows.length
    ? `Page ${state.page + 1} of ${pages} · ${rows.length} member${rows.length > 1 ? "s" : ""}`
    : "No results";
  $("[data-prev]").disabled = state.page === 0;
  $("[data-next]").disabled = state.page >= pages - 1;
  $("[data-roster-count]").textContent = members.length;
}

/* ── Toolbar / pager ─────────────────────────────────────────────────────── */
$("[data-search]").addEventListener("input", (e) => { state.search = e.target.value; state.page = 0; renderTable(); });
$("[data-filter]").addEventListener("change", (e) => { state.status = e.target.value; state.page = 0; renderTable(); });
$("[data-prev]").addEventListener("click", () => { if (state.page > 0) { state.page--; renderTable(); } });
$("[data-next]").addEventListener("click", () => { state.page++; renderTable(); });

/* ── Nav ─────────────────────────────────────────────────────────────────── */
$$("[data-nav]").forEach((btn) => btn.addEventListener("click", () => {
  $$("[data-nav]").forEach((b) => b.classList.toggle("is-active", b === btn));
  $$(".main").forEach((m) => (m.hidden = m.dataset.section !== btn.dataset.nav));
}));

/* ── Drawer (member detail) ──────────────────────────────────────────────── */
let activeId = null;
const drawer = $("[data-drawer]"), scrim = $("[data-scrim]");

function openDrawer(id) {
  const m = members.find((x) => x.id === id);
  if (!m) return;
  activeId = id;
  $("[data-d-name]").textContent = fullName(m);
  $("[data-d-number]").textContent = m.membershipNumber;
  $("[data-d-email]").textContent = m.email;
  $("[data-d-phone]").textContent = m.phone;
  $("[data-d-joined]").textContent = m.joinYear;
  const st = $("[data-d-status]"); st.textContent = titleCase(m.status); st.className = `badge badge--${m.status.toLowerCase()}`;

  // status select: current + allowed transitions (mirrors MemberStatus.canTransitionTo)
  const allowed = STATUS_TRANSITIONS[m.status];
  const sel = $("[data-status-select]");
  sel.innerHTML = [m.status, ...allowed].map((s) => `<option value="${s}">${titleCase(s)}</option>`).join("");
  const terminal = allowed.length === 0;
  sel.disabled = terminal;
  $("[data-apply-status]").disabled = terminal;
  $("[data-status-note]").hidden = !terminal;

  renderDrawerDues(m);
  scrim.hidden = false; drawer.hidden = false;
}
function closeDrawer() { drawer.hidden = true; scrim.hidden = true; activeId = null; }

function renderDrawerDues(m) {
  const years = Object.keys(m.dues).map(Number).sort((a, b) => b - a);
  $("[data-d-dues]").innerHTML = years.map((y) => {
    const d = m.dues[y];
    const cell = d.paid
      ? `<span class="pill pill--paid">Paid</span></td><td class="${d.paidDate ? "" : "muted"}">${d.paidDate ? shortDate(d.paidDate) : "—"}`
      : `<span class="pill pill--unpaid">Unpaid</span></td><td><button class="btn btn--sm" data-pay-year="${y}"><span>Mark paid</span></button>`;
    return `<tr><td>${y}</td><td>${cell}</td></tr>`;
  }).join("");
}

$("[data-rows]").addEventListener("click", (e) => { const tr = e.target.closest("tr[data-id]"); if (tr) openDrawer(tr.dataset.id); });
$("[data-close-drawer]").addEventListener("click", closeDrawer);
scrim.addEventListener("click", closeDrawer);
document.addEventListener("keydown", (e) => { if (e.key === "Escape") { closeDrawer(); closeModal(); } });

/* change status */
$("[data-apply-status]").addEventListener("click", () => {
  const m = members.find((x) => x.id === activeId); if (!m) return;
  const next = $("[data-status-select]").value;
  if (next === m.status) { toast("No change — status is already " + titleCase(next) + "."); return; }
  m.status = next;                       // PATCH /api/members/{id}/status
  openDrawer(m.id); renderTable();
  toast(`${fullName(m)} is now ${titleCase(next)}.`);
});

/* record dues payment */
$("[data-d-dues]").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-pay-year]"); if (!btn) return;
  const m = members.find((x) => x.id === activeId); if (!m) return;
  const y = btn.dataset.payYear;
  m.dues[y] = { paid: true, paidDate: TODAY };     // POST /api/members/{id}/dues/{year}/pay
  let note = `Dues ${y} recorded for ${fullName(m)}.`;
  if (m.status === "INACTIVE") { m.status = "ACTIVE"; note += " Reactivated."; }   // backend reactivates
  openDrawer(m.id); renderTable();
  toast(note);
});

/* ── Register modal ──────────────────────────────────────────────────────── */
const modal = $("[data-modal]"), regForm = $("[data-register]");
function openModal() { modal.hidden = false; setTimeout(() => regForm.firstName.focus(), 50); }
function closeModal() { if (modal.hidden) return; modal.hidden = true; regForm.reset(); $$(".err", regForm).forEach((s) => s.removeAttribute("data-shown")); $("[data-msg]", regForm).hidden = true; }
$("[data-open-register]").addEventListener("click", openModal);
$$("[data-close-modal]").forEach((el) => el.addEventListener("click", closeModal));

function nextNumber() {
  const n = members.filter((m) => m.membershipNumber.startsWith(`TEC-${YEAR}-`))
    .reduce((max, m) => Math.max(max, +m.membershipNumber.split("-")[2]), 0);
  return `TEC-${YEAR}-${String(n + 1).padStart(3, "0")}`;
}

regForm.addEventListener("submit", (e) => {
  e.preventDefault();
  const f = regForm; let ok = true;
  const set = (name, msg) => { const row = f[name].closest(".field-row"); const slot = $(".err", row); if (slot) { slot.textContent = msg || ""; slot.toggleAttribute("data-shown", !!msg); } if (msg) ok = false; };
  set("firstName", f.firstName.value.trim() ? "" : "Required.");
  set("lastName",  f.lastName.value.trim() ? "" : "Required.");
  set("dateOfBirth", f.dateOfBirth.value ? "" : "Required.");
  const email = f.email.value.trim();
  set("email", !email ? "Required." : !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email) ? "Enter a valid email." : "");
  if (members.some((m) => m.email.toLowerCase() === email.toLowerCase())) {
    const box = $("[data-msg]", f); box.className = "msg msg--error"; box.textContent = "A member with that email already exists."; box.hidden = false; ok = false;
  }
  if (!ok) return;

  const m = {                                   // POST /api/members
    id: "m_" + Date.now(),
    membershipNumber: nextNumber(),
    firstName: f.firstName.value.trim(), lastName: f.lastName.value.trim(),
    email, phone: f.phone.value.trim() || "—", joinYear: String(YEAR),
    status: "ACTIVE",
    dues: { [YEAR]: { paid: false, paidDate: null } },   // initial unpaid record
  };
  members.unshift(m);
  state.search = ""; $("[data-search]").value = ""; state.status = ""; $("[data-filter]").value = ""; state.page = 0;
  renderTable(); closeModal();
  toast(`${fullName(m)} registered as ${m.membershipNumber}.`);
});

/* ── Announcements ───────────────────────────────────────────────────────── */
const recipientCount = (group) =>
  members.filter((m) => group === "ACTIVE_MEMBERS" ? m.status === "ACTIVE" : m.status !== "TERMINATED").length;

function refreshAnnounceCounts() {
  $("[data-count-active]").textContent = `(${recipientCount("ACTIVE_MEMBERS")})`;
  $("[data-count-all]").textContent = `(${recipientCount("ALL_MEMBERS")})`;
}

const announce = $("[data-announce]");
announce.addEventListener("submit", (e) => {
  e.preventDefault();
  const f = announce; let ok = true;
  const set = (name, msg) => { const slot = $(".err", f[name].closest(".field-row")); if (slot) { slot.textContent = msg || ""; slot.toggleAttribute("data-shown", !!msg); } if (msg) ok = false; };
  set("subject", f.subject.value.trim() ? "" : "Subject is required.");
  set("body", f.body.value.trim() ? "" : "Message is required.");
  if (!ok) return;
  const group = f.group.value;
  const count = recipientCount(group);                 // POST /api/announcements -> { recipientCount }
  const box = $("[data-msg]", f);
  box.className = "msg msg--success";
  box.textContent = `Announcement sent to ${count} member${count === 1 ? "" : "s"}.`;
  box.hidden = false;
  f.subject.value = ""; f.body.value = "";
  toast(`Announcement queued for ${count} member${count === 1 ? "" : "s"}.`);
});

/* ── Boot ─────────────────────────────────────────────────────────────────── */
renderTable();
refreshAnnounceCounts();

/* deep links (handy for demos): #announcements, #register, #member */
const hash = location.hash;
if (hash === "#announcements") $('[data-nav="announcements"]').click();
else if (hash === "#register") openModal();
else if (hash === "#member") openDrawer(members[0].id);
