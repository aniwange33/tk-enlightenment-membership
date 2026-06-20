import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import PasswordInput from "../components/PasswordInput";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";

const isEmail = (v: string) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v.trim());

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!isEmail(email)) return setError("Enter a valid email address.");
    if (password.length < 8) return setError("Password must be at least 8 characters.");
    setPending(true);
    try {
      const { mustChangePassword } = await login(email, password);
      nav(mustChangePassword ? "/change-password" : "/", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401 ? "Invalid email or password." : (err as Error).message);
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthLayout>
      <form onSubmit={onSubmit} noValidate>
        <p className="eyebrow rise" style={{ animationDelay: ".14s" }}>Welcome back</p>
        <h2 className="heading rise" style={{ animationDelay: ".2s" }}>Sign in</h2>
        <p className="lede rise" style={{ animationDelay: ".26s" }}>Enter your member credentials to continue.</p>

        {error && <div className="msg msg--error">{error}</div>}

        <label className="field-row rise" style={{ animationDelay: ".32s" }}>
          <span className="label">Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                 placeholder="you@taraku.org" autoComplete="username" />
        </label>
        <label className="field-row rise" style={{ animationDelay: ".38s" }}>
          <span className="label">Password</span>
          <PasswordInput value={password} onChange={setPassword} placeholder="••••••••" autoComplete="current-password" />
        </label>

        <button className="btn rise" style={{ animationDelay: ".44s" }} type="submit" disabled={pending}>
          <span>{pending ? "Signing in…" : "Enter"}</span><i className="btn__spark" />
        </button>
        <div className="alt rise" style={{ animationDelay: ".5s" }}>
          <Link to="/forgot">Forgot your password?</Link>
        </div>
      </form>
    </AuthLayout>
  );
}
