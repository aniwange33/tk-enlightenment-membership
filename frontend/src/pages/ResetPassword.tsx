import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import PasswordInput from "../components/PasswordInput";
import { authApi } from "../api/client";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const nav = useNavigate();
  const [next, setNext] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (next.length < 8) return setError("Password must be at least 8 characters.");
    setPending(true);
    try {
      await authApi.resetPassword(token, next);
      nav("/login", { replace: true, state: { reset: true } });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthLayout>
      <form onSubmit={onSubmit} noValidate>
        <p className="eyebrow">Recover access</p>
        <h2 className="heading">Set a new password</h2>
        <p className="lede">Choose a new password to finish resetting your account.</p>

        {!token && <div className="msg msg--error">This reset link is missing its token. Request a new link.</div>}
        {error && <div className="msg msg--error">{error}</div>}

        <label className="field-row">
          <span className="label">New password</span>
          <PasswordInput value={next} onChange={setNext} placeholder="at least 8 characters" autoComplete="new-password" />
        </label>

        <button className="btn" type="submit" disabled={pending || !token}>
          <span>{pending ? "Resetting…" : "Reset password"}</span><i className="btn__spark" />
        </button>
        <div className="alt"><Link to="/login">← Back to sign in</Link></div>
      </form>
    </AuthLayout>
  );
}
