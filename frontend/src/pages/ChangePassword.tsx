import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import PasswordInput from "../components/PasswordInput";
import { useAuth } from "../auth/AuthContext";

export default function ChangePassword() {
  const { changePassword } = useAuth();
  const nav = useNavigate();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!current) return setError("Enter your current password.");
    if (next.length < 8) return setError("New password must be at least 8 characters.");
    setPending(true);
    try {
      await changePassword(current, next);
      nav("/", { replace: true });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthLayout>
      <form onSubmit={onSubmit} noValidate>
        <p className="eyebrow">First sign-in</p>
        <h2 className="heading">Choose a new password</h2>
        <p className="lede">For your security, set a personal password before you continue.</p>

        {error && <div className="msg msg--error">{error}</div>}

        <label className="field-row">
          <span className="label">Current password</span>
          <PasswordInput value={current} onChange={setCurrent} placeholder="temporary password" autoComplete="current-password" />
        </label>
        <label className="field-row">
          <span className="label">New password</span>
          <PasswordInput value={next} onChange={setNext} placeholder="at least 8 characters" autoComplete="new-password" />
        </label>

        <button className="btn" type="submit" disabled={pending}>
          <span>{pending ? "Saving…" : "Set password & continue"}</span><i className="btn__spark" />
        </button>
      </form>
    </AuthLayout>
  );
}
