/* ============================================================================
   Member portal — mock data + interactions, mirroring the real contracts:
     GET  /api/me            -> profile (MeResponse)
     PUT  /api/me            -> { phone, address }  (only these are editable)
     GET  /api/me/dues       -> [{ year, paid, paidDate }]
   Swap the mock* functions for fetch() with the Bearer token to wire the API.
   ========================================================================== */

const $  = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

/* ── Mock backend ─────────────────────────────────────────────────────────── */
const me = {
  membershipNumber: "TEC-2021-014",
  firstName: "Amara", lastName: "Okonkwo",
  email: "amara@taraku.org",
  dateOfBirth: "1989-03-14",
  joinDate: "2021-02-02",
  phone: "+234 802 555 0114",
  address: "14 Lantern Way, Makurdi, Benue",
  status: "ACTIVE",
};
const dues = [
  { year: 2026, paid: false, paidDate: null },
  { year: 2025, paid: true,  paidDate: "2025-03-09" },
  { year: 2024, paid: true,  paidDate: "2024-02-27" },
  { year: 2023, paid: true,  paidDate: "2023-04-11" },
  { year: 2022, paid: true,  paidDate: "2022-03-30" },
];

const wait = (ms) => new Promise((r) => setTimeout(r, ms));
async function mockUpdateMe(patch) { await wait(550); Object.assign(me, patch); return { ...me }; }

/* ── Formatting ───────────────────────────────────────────────────────────── */
const MONTHS = ["January","February","March","April","May","June","July","August","September","October","November","December"];
function longDate(iso) { const d = new Date(iso + "T00:00:00"); return `${String(d.getDate()).padStart(2,"0")} ${MONTHS[d.getMonth()]} ${d.getFullYear()}`; }
const titleCase = (s) => s.charAt(0) + s.slice(1).toLowerCase();

/* ── Render ───────────────────────────────────────────────────────────────── */
function renderProfile() {
  const full = `${me.firstName} ${me.lastName}`;
  $$("[data-name]").forEach((el) => (el.textContent = full));
  $$("[data-number]").forEach((el) => (el.textContent = me.membershipNumber));
  $("[data-initials]").textContent = (me.firstName[0] + me.lastName[0]).toUpperCase();
  $("[data-since]").textContent = me.joinDate.slice(0, 4);
  $('[data-field="email"]').textContent = me.email;
  $('[data-view-value="phone"]').textContent = me.phone;
  $('[data-view-value="address"]').textContent = me.address;

  const badge = $("[data-status]");
  badge.textContent = titleCase(me.status);
  badge.className = `badge badge--${me.status.toLowerCase()}`;
}

function renderDues() {
  const tbody = $("[data-dues]");
  tbody.innerHTML = dues
    .map((d) => `<tr>
      <td>${d.year}</td>
      <td><span class="pill pill--${d.paid ? "paid" : "unpaid"}">${d.paid ? "Paid" : "Unpaid"}</span></td>
      <td class="${d.paidDate ? "" : "muted"}">${d.paidDate ? longDate(d.paidDate) : "—"}</td>
    </tr>`).join("");
  const outstanding = dues.filter((d) => !d.paid).length;
  $("[data-dues-summary]").textContent = outstanding
    ? `${outstanding} year${outstanding > 1 ? "s" : ""} outstanding`
    : "All dues settled";
}

/* ── Edit contact details ─────────────────────────────────────────────────── */
const form = $("[data-profile]");
const actions = $("[data-actions]");
const msg = $("[data-msg]");

function setEditing(on) {
  form.classList.toggle("is-editing", on);
  $$(".row__edit", form).forEach((el) => (el.hidden = !on));
  actions.hidden = !on;
  $("[data-edit]").style.visibility = on ? "hidden" : "visible";
  if (on) { msg.hidden = true; $('input[name="phone"]', form).focus(); }
}

$("[data-edit]").addEventListener("click", () => setEditing(true));
$("[data-cancel]").addEventListener("click", () => {
  form.phone.value = me.phone; form.address.value = me.address;
  setEditing(false);
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const phone = form.phone.value.trim();
  const address = form.address.value.trim();
  if (!phone || !address) {
    msg.className = "msg msg--error"; msg.textContent = "Phone and address can't be empty."; msg.hidden = false;
    return;
  }
  const btn = e.submitter, span = $("span", btn);
  btn.disabled = true; span.textContent = "Saving…";
  try {
    await mockUpdateMe({ phone, address });   // PUT /api/me
    renderProfile();
    setEditing(false);
    msg.className = "msg msg--success"; msg.textContent = "Your contact details were updated."; msg.hidden = false;
  } finally { btn.disabled = false; span.textContent = "Save changes"; }
});

/* ── Boot ─────────────────────────────────────────────────────────────────── */
renderProfile();
renderDues();
if (location.hash === "#edit") setEditing(true);
