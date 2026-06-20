import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

interface Props {
  section: string;
  name: string;
  sub?: string;
  initials: string;
}

export default function TopBar({ section, name, sub, initials }: Props) {
  const { logout } = useAuth();
  const nav = useNavigate();

  function signOut() {
    logout();
    nav("/login", { replace: true });
  }

  return (
    <header className="topbar">
      <Link className="topbar__brand" to="/" aria-label="Taraku home">
        <img className="emblem emblem--sm" src="/tk-logo.jpg" alt="" width={34} height={34} />
        <span className="topbar__mark">Taraku</span>
        <span className="topbar__div" />
        <span className="topbar__sec">{section}</span>
      </Link>
      <div className="topbar__user">
        <span className="who">
          <span className="who__name">{name}</span>
          {sub && <span className="who__no">{sub}</span>}
        </span>
        <span className="avatar" aria-hidden="true">{initials}</span>
        <button className="btn btn--ghost btn--sm" onClick={signOut}><span>Sign out</span></button>
      </div>
    </header>
  );
}
