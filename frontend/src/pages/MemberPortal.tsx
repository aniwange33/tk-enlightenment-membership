import { useEffect, useState, type FormEvent } from "react";
import TopBar from "../components/TopBar";
import StatusBadge from "../components/StatusBadge";
import CenterNote from "../components/CenterNote";
import Foot from "../components/Foot";
import { meApi } from "../api/client";
import type { DuesRecord, Member } from "../api/types";
import { fullName, initialsOf, longDate, shortDate, yearOf } from "../util/format";

export default function MemberPortal() {
  const [member, setMember] = useState<Member | null>(null);
  const [dues, setDues] = useState<DuesRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadErr, setLoadErr] = useState<string | null>(null);

  const [editing, setEditing] = useState(false);
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState<{ type: "error" | "success"; text: string } | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const [m, d] = await Promise.all([meApi.get(), meApi.dues()]);
        setMember(m);
        setDues([...d].sort((a, b) => b.year - a.year));
        setPhone(m.phone ?? "");
        setAddress(m.address ?? "");
      } catch (e) {
        setLoadErr((e as Error).message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <CenterNote text="Loading your portal…" />;
  if (loadErr || !member) return <CenterNote text={loadErr ?? "Could not load profile."} />;

  function startEdit() {
    setEditing(true);
    setMsg(null);
  }
  function cancelEdit() {
    if (!member) return;
    setPhone(member.phone ?? "");
    setAddress(member.address ?? "");
    setEditing(false);
  }
  async function onSave(e: FormEvent) {
    e.preventDefault();
    setMsg(null);
    if (!phone.trim() || !address.trim()) return setMsg({ type: "error", text: "Phone and address can't be empty." });
    setSaving(true);
    try {
      const updated = await meApi.update(phone.trim(), address.trim());
      setMember(updated);
      setEditing(false);
      setMsg({ type: "success", text: "Your contact details were updated." });
    } catch (e) {
      setMsg({ type: "error", text: (e as Error).message });
    } finally {
      setSaving(false);
    }
  }

  const outstanding = dues.filter((d) => !d.paid).length;

  return (
    <div className="app">
      <div className="field" aria-hidden="true"><div className="halo halo--portal" /><div className="grain" /></div>
      <TopBar section="Members" name={fullName(member)} sub={member.membershipNumber} initials={initialsOf(member)} />

      <main className="shell">
        <section className="card rise" style={{ animationDelay: ".05s" }} aria-label="Membership card">
          <div className="card__sheen" aria-hidden="true" />
          <div className="card__top">
            <span className="card__club">Taraku Enlightenment Club</span>
            <img className="emblem emblem--card" src="/tk-logo.jpg" alt="" width={46} height={46} />
          </div>
          <div className="card__body">
            <span className="card__label">Member</span>
            <h1 className="card__name">{fullName(member)}</h1>
            <span className="card__number">{member.membershipNumber}</span>
          </div>
          <div className="card__foot">
            <span className="card__since">Member since <b>{yearOf(member.joinDate)}</b></span>
            <StatusBadge status={member.status} />
          </div>
        </section>

        <div className="grid">
          <section className="block rise" style={{ animationDelay: ".14s" }}>
            <div className="block__head">
              <h2 className="block__title">My profile</h2>
              {!editing && <button className="link-btn" onClick={startEdit}><span>Edit contact details</span></button>}
            </div>

            {msg && <div className={`msg msg--${msg.type}`}>{msg.text}</div>}

            <form className="rows" onSubmit={onSave}>
              <div className="row"><span className="row__k">Membership&nbsp;no.</span><span className="row__v" data-mono="">{member.membershipNumber}</span></div>
              <div className="row"><span className="row__k">Email</span><span className="row__v">{member.email}</span></div>
              <div className="row"><span className="row__k">Date of birth</span><span className="row__v">{longDate(member.dateOfBirth)}</span></div>
              <div className="row"><span className="row__k">Joined</span><span className="row__v">{longDate(member.joinDate)}</span></div>

              <div className="row">
                <span className="row__k">Phone</span>
                {editing
                  ? <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} autoComplete="tel" />
                  : <span className="row__v">{member.phone ?? "—"}</span>}
              </div>
              <div className="row">
                <span className="row__k">Address</span>
                {editing
                  ? <input type="text" value={address} onChange={(e) => setAddress(e.target.value)} autoComplete="street-address" />
                  : <span className="row__v">{member.address ?? "—"}</span>}
              </div>

              {editing && (
                <div className="rows__actions">
                  <button className="btn" type="submit" disabled={saving}><span>{saving ? "Saving…" : "Save changes"}</span><i className="btn__spark" /></button>
                  <button className="btn btn--ghost" type="button" onClick={cancelEdit}><span>Cancel</span></button>
                </div>
              )}
            </form>
            <p className="note">Only your phone and address can be changed here. Other details are kept by the club.</p>
          </section>

          <section className="block rise" style={{ animationDelay: ".22s" }}>
            <div className="block__head">
              <h2 className="block__title">My dues</h2>
              <span className="block__meta">{outstanding ? `${outstanding} year${outstanding > 1 ? "s" : ""} outstanding` : "All dues settled"}</span>
            </div>
            <table className="dues">
              <thead><tr><th>Year</th><th>Status</th><th>Paid on</th></tr></thead>
              <tbody>
                {dues.map((d) => (
                  <tr key={d.year}>
                    <td>{d.year}</td>
                    <td><span className={`pill pill--${d.paid ? "paid" : "unpaid"}`}>{d.paid ? "Paid" : "Unpaid"}</span></td>
                    <td className={d.paidDate ? "" : "muted"}>{d.paidDate ? shortDate(d.paidDate) : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="note">Dues are recorded by club officers. Questions? Contact the secretariat.</p>
          </section>
        </div>
      </main>
      <Foot tag="Member Portal" />
    </div>
  );
}
