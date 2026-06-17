export default function Foot({ tag = "Members" }: { tag?: string }) {
  return (
    <footer className="foot">
      <span>Taraku Enlightenment Club</span>
      <span className="foot__dot">·</span>
      <span>{tag}</span>
      <span className="foot__dot">·</span>
      <span className="foot__tag">react</span>
    </footer>
  );
}
