/* ============================================================================
   Taraku · Luminary auth prototype
   Pure front-end. Mock logic mirrors the real REST contracts so it ports cleanly:
     POST /api/auth/login            -> { token, mustChangePassword }
     POST /api/auth/change-password  -> { token, mustChangePassword }
     POST /api/auth/forgot-password  -> { message } | 404 unknown | 403 disabled
     POST /api/auth/reset-password   -> { message } | 400 invalid/expired token
   Swap the mock* functions for fetch() calls to wire the backend.
   ========================================================================== */

const $  = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

const VIEWS = ["login", "change", "forgot", "reset", "done"];
const root = document.documentElement;
let resetToken = null;

/* ── View routing ─────────────────────────────────────────────────────────── */
function setView(name, { push = true } = {}) {
  if (!VIEWS.includes(name)) name = "login";
  $$(".view").forEach((v) => {
    const active = v.dataset.view === name;
    v.hidden = !active;
    if (active) {
      v.classList.remove("is-entering");
      void v.offsetWidth;            // restart the entrance animation
      v.classList.add("is-entering");
      clearMessage(v);
      const first = $("input", v);
      if (first) setTimeout(() => first.focus({ preventScroll: true }), 60);
    }
  });
  root.dataset.view = name;
  if (push && location.hash.slice(1) !== name) history.replaceState(null, "", `#${name}`);
}

window.addEventListener("hashchange", () => setView(location.hash.slice(1)));

/* anchor / button navigation */
document.addEventListener("click", (e) => {
  const go = e.target.closest("[data-go]");
  if (go) { e.preventDefault(); setView(go.dataset.go); }
});

/* ── Field + message helpers ──────────────────────────────────────────────── */
function fieldError(input, msg) {
  const row = input.closest(".field-row");
  const slot = row && $(".err", row);
  if (!slot) return;
  if (msg) { slot.textContent = msg; slot.setAttribute("data-shown", ""); }
  else { slot.textContent = ""; slot.removeAttribute("data-shown"); }
}
function clearErrors(form) { $$(".err", form).forEach((s) => { s.textContent = ""; s.removeAttribute("data-shown"); }); }

function showMessage(form, type, text) {
  const box = $("[data-msg]", form);
  if (!box) return;
  box.className = `msg msg--${type}`;
  box.textContent = text;
  box.hidden = false;
}
function clearMessage(form) { const box = $("[data-msg]", form); if (box) box.hidden = true; }

const isEmail = (v) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v.trim());

async function withPending(btn, label, fn) {
  const span = $("span", btn);
  const original = span.textContent;
  btn.disabled = true; span.textContent = label;
  try { return await fn(); }
  finally { btn.disabled = false; span.textContent = original; }
}

/* ── Mock backend (replace with fetch) ────────────────────────────────────── */
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

async function mockLogin(email, password) {
  await wait(650);
  if (email === "nobody@taraku.org") { const e = new Error("Invalid email or password"); e.status = 401; throw e; }
  // first@taraku.org demonstrates the forced first-login change
  return { token: "mock.jwt.token", mustChangePassword: email === "first@taraku.org", admin: email.startsWith("admin") };
}
async function mockForgot(email) {
  await wait(650);
  if (email === "unknown@taraku.org") { const e = new Error("No account found for that email"); e.status = 404; throw e; }
  if (email === "left@taraku.org")    { const e = new Error("Account is disabled"); e.status = 403; throw e; }
  return { message: "Reset link sent" };
}
async function mockReset(token, newPassword) {
  await wait(650);
  if (!token || token === "expired") { const e = new Error("Invalid or expired reset token"); e.status = 400; throw e; }
  return { message: "Password updated" };
}

/* ── Login ────────────────────────────────────────────────────────────────── */
$('[data-view="login"]').addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.currentTarget; clearErrors(form); clearMessage(form);
  const email = form.email.value.trim(), password = form.password.value;
  let ok = true;
  if (!isEmail(email)) { fieldError(form.email, "Enter a valid email address."); ok = false; }
  if (password.length < 8) { fieldError(form.password, "Password must be at least 8 characters."); ok = false; }
  if (!ok) return;

  try {
    const res = await withPending(e.submitter, "Signing in…", () => mockLogin(email, password));
    if (res.mustChangePassword) { showMessage($('[data-view="change"]'), "success", "Welcome — set a new password to continue."); setView("change"); }
    else { finish(res.admin ? "Welcome, Secretary" : "Welcome back", res.admin ? "Opening the admin console…" : "Opening your member portal…"); }
  } catch (err) { showMessage(form, "error", err.status === 401 ? "Invalid email or password." : err.message); }
});

/* ── Change password (first-run) ──────────────────────────────────────────── */
$('[data-view="change"]').addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.currentTarget; clearErrors(form); clearMessage(form);
  let ok = true;
  if (!form.currentPassword.value) { fieldError(form.currentPassword, "Enter your current password."); ok = false; }
  if (form.newPassword.value.length < 8) { fieldError(form.newPassword, "Password must be at least 8 characters."); ok = false; }
  if (!ok) return;
  await withPending(e.submitter, "Saving…", () => wait(650));
  finish("Password set", "Your account is secured. Opening your portal…");
});

/* ── Forgot ───────────────────────────────────────────────────────────────── */
$('[data-view="forgot"]').addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.currentTarget; clearErrors(form); clearMessage(form);
  const email = form.email.value.trim();
  if (!isEmail(email)) { fieldError(form.email, "Enter a valid email address."); return; }
  try {
    await withPending(e.submitter, "Sending…", () => mockForgot(email));
    showMessage(form, "success", `If ${email} is registered, a reset link is on its way. It expires in 24 hours.`);
    form.email.value = "";
  } catch (err) {
    showMessage(form, "error", err.status === 404 ? "We couldn't find an account for that email."
                              : err.status === 403 ? "That account is disabled — contact the secretariat."
                              : err.message);
  }
});

/* ── Reset ────────────────────────────────────────────────────────────────── */
$('[data-view="reset"]').addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.currentTarget; clearErrors(form); clearMessage(form);
  if (form.newPassword.value.length < 8) { fieldError(form.newPassword, "Password must be at least 8 characters."); return; }
  try {
    await withPending(e.submitter, "Resetting…", () => mockReset(resetToken, form.newPassword.value));
    finish("Password reset", "You can now sign in with your new password.");
  } catch (err) { showMessage(form, "error", err.message); }
});

/* ── Done / sign-out ──────────────────────────────────────────────────────── */
function finish(title, sub) {
  $("[data-done-title]").textContent = title;
  $("[data-done-sub]").textContent = sub;
  setView("done");
}

/* ── Password show/hide ───────────────────────────────────────────────────── */
document.addEventListener("click", (e) => {
  const t = e.target.closest("[data-toggle]");
  if (!t) return;
  const input = $("input", t.parentElement);
  const show = input.type === "password";
  input.type = show ? "text" : "password";
  t.textContent = show ? "hide" : "show";
  t.setAttribute("aria-label", show ? "Hide password" : "Show password");
});

/* ── Strength meter ───────────────────────────────────────────────────────── */
document.addEventListener("input", (e) => {
  const input = e.target.closest("[data-strength]");
  if (!input) return;
  const meter = $("[data-meter]", input.closest(".field-row"));
  if (!meter) return;
  const v = input.value;
  let score = 0;
  if (v.length >= 8) score++;
  if (/[A-Z]/.test(v) && /[a-z]/.test(v)) score++;
  if (/\d|[^\w]/.test(v)) score++;
  meter.dataset.level = v ? String(Math.max(1, score)) : "0";
});

/* ── Rotating club tenets ─────────────────────────────────────────────────── */
const TENETS = [
  "“Knowledge kept is a candle; knowledge shared is the dawn.”",
  "“We gather not to be taught, but to be lit.”",
  "“A mind, once widened, keeps the shape of the light.”",
  "“Every member, a lantern. Together, a sunrise.”",
];
const tenetEl = $("#tenet");
if (tenetEl && !matchMedia("(prefers-reduced-motion: reduce)").matches) {
  let i = 0;
  setInterval(() => {
    i = (i + 1) % TENETS.length;
    tenetEl.style.transition = "opacity .5s ease";
    tenetEl.style.opacity = "0";
    setTimeout(() => { tenetEl.textContent = TENETS[i]; tenetEl.style.opacity = "1"; }, 500);
  }, 6500);
}

/* ── Boot ─────────────────────────────────────────────────────────────────── */
(function boot() {
  const params = new URLSearchParams(location.search);
  resetToken = params.get("token");
  const initial = location.hash.slice(1);
  if (resetToken && !initial) setView("reset", { push: false });
  else setView(VIEWS.includes(initial) ? initial : "login", { push: false });
})();
