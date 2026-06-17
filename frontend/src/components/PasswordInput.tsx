import { useState } from "react";

interface Props {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  autoComplete?: string;
  name?: string;
}

export default function PasswordInput({ value, onChange, placeholder, autoComplete, name }: Props) {
  const [show, setShow] = useState(false);
  return (
    <span className="pw">
      <input
        type={show ? "text" : "password"}
        name={name}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
      />
      <button
        type="button"
        className="pw__toggle"
        aria-label={show ? "Hide password" : "Show password"}
        onClick={() => setShow((s) => !s)}
      >
        {show ? "hide" : "show"}
      </button>
    </span>
  );
}
