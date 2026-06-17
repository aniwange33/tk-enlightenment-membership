import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { type ReactNode } from "react";
import { useAuth } from "./auth/AuthContext";
import type { Role } from "./api/types";
import Login from "./pages/Login";
import ChangePassword from "./pages/ChangePassword";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import MemberPortal from "./pages/MemberPortal";
import AdminConsole from "./pages/AdminConsole";

/** Authenticated + past the must-change gate, optionally role-restricted. */
function Protected({ role, children }: { role?: Role; children: ReactNode }) {
  const { isAuthenticated, mustChangePassword, role: myRole } = useAuth();
  const loc = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  if (mustChangePassword) return <Navigate to="/change-password" replace />;
  if (role && myRole !== role) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function Home() {
  const { role } = useAuth();
  return <Navigate to={role === "ADMIN" ? "/admin" : "/member"} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot" element={<ForgotPassword />} />
      <Route path="/reset" element={<ResetPassword />} />
      <Route path="/change-password" element={<RequireAuth><ChangePassword /></RequireAuth>} />
      <Route path="/" element={<Protected><Home /></Protected>} />
      <Route path="/member" element={<Protected role="MEMBER"><MemberPortal /></Protected>} />
      <Route path="/admin" element={<Protected role="ADMIN"><AdminConsole /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
