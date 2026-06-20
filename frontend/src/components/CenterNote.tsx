export default function CenterNote({ text }: { text: string }) {
  return (
    <div className="app">
      <div className="field" aria-hidden="true"><div className="halo" /><div className="grain" /></div>
      <div style={{ flex: 1, display: "grid", placeItems: "center", padding: "2rem", position: "relative", zIndex: 1 }}>
        <p className="lede">{text}</p>
      </div>
    </div>
  );
}
