import { type ReactNode } from "react";
import Foot from "./Foot";

/** The Luminary split layout: brand aside (emblem + wordmark + tenet) and a form panel. */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <div className="field" aria-hidden="true">
        <div className="halo" />
        <div className="grain" />
      </div>
      <main className="frame">
        <aside className="aside">
          <div className="brand">
            <img
              className="emblem emblem--lg rise"
              style={{ animationDelay: ".04s" }}
              src="/tk-logo.jpg"
              alt="Taraku Enlightenment Club emblem"
              width={96}
              height={96}
            />
            <span className="brand__eyebrow rise" style={{ animationDelay: ".12s" }}>
              · United to Enlighten ·
            </span>
            <h1 className="brand__mark rise" style={{ animationDelay: ".18s" }}>
              Taraku
            </h1>
            <p className="brand__sub rise" style={{ animationDelay: ".24s" }}>
              Enlightenment&nbsp;Club
            </p>
          </div>
          <figure className="tenet rise" style={{ animationDelay: ".36s" }}>
            <blockquote>“Knowledge kept is a candle; knowledge shared is the dawn.”</blockquote>
            <figcaption>— Club Tenet</figcaption>
          </figure>
        </aside>
        <section className="panel">{children}</section>
      </main>
      <Foot tag="Members' Access" />
    </>
  );
}
