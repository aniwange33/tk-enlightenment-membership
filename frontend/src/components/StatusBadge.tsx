import type { MemberStatus } from "../api/types";
import { titleCase } from "../util/format";

export default function StatusBadge({ status }: { status: MemberStatus }) {
  return <span className={`badge badge--${status.toLowerCase()}`}>{titleCase(status)}</span>;
}
