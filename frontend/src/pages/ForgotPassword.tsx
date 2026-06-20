import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { authApi, ApiError } from "../api/client";

const isEmail = (v: string) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v.trim());

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [msg, setMsg] = useState<{ type: "error" | "success"; text: string } | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setMsg(null);
    if (!isEmail(email)) return setMsg({ type: "error", text: "Enter a valid email address." });
    setPending(true);
    try {
      await authApi.forgotPassword(email.trim());
      setMsg({ type: "success", text: `If ${email.trim()} is registered, a reset link is on its way. It expires in 24 hours.` });
      setEmail("");
    } catch (err) {
      const text =
        err instanceof ApiError && err.status === 404 ? "We couldn't find an account for that email."
        : err instanceof ApiError && err.status === 403 ? "That account is disabled — contact the secretariat."
        : (err as Error).message;
      setMsg({ type: "error", text });
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthLayout>
      <form onSubmit={onSubmit} noValidate>
        <p className="eyebrow">Recover access</p>
        <h2 className="heading">Forgot password</h2>
        <p className="lede">We'll send a reset link to your registered email. It expires in 24&nbsp;hours.</p>

        {msg && <div className={`msg msg--${msg.type}`}>{msg.text}</div>}

        <label className="field-row">
          <span className="label">Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                 placeholder="you@taraku.org" autoComplete="username" />
        </label>

        <button className="btn" type="submit" disabled={pending}>
          <span>{pending ? "Sending…" : "Send reset link"}</span><i className="btn__spark" />
        </button>
        <div className="alt"><Link to="/login">← Back to sign in</Link></div>
      </form>
    </AuthLayout>
  );
}
