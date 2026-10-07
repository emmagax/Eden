import { useState, type ReactNode } from "react";
import { Navigate } from "react-router";
import { useAuth } from "./AuthContext";

export function SessionGate({ children, protectedRoute = false }: { children: ReactNode; protectedRoute?: boolean }) {
  const session = useAuth();
  if (!session) throw new Error("SessionGate requires AuthProvider");
  if (session.loading) return <main><p role="status">Checking your session…</p></main>;
  if (session.error) return <main><p role="alert">{session.error}</p><button onClick={() => void session.restore()}>Try again</button></main>;
  if (protectedRoute && !session.user) return <Navigate to="/login" replace />;
  if (!protectedRoute && session.user) return <Navigate to="/account" replace />;
  return children;
}

export function AccountPage() {
  const session = useAuth();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function signOut() {
    setBusy(true); setError("");
    try { await session!.signOut(); }
    catch { setError("Unable to sign out. Please try again."); }
    finally { setBusy(false); }
  }
  return <main><section className="auth-panel"><h1>Your account</h1>
    <p>Signed in as {session?.user?.username}.</p>
    {error && <p role="alert">{error}</p>}
    <button disabled={busy} onClick={() => void signOut()}>Sign out</button>
  </section></main>;
}
