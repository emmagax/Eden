import { useCallback, useEffect, useState, type ReactNode } from "react";
import { currentUser, logout, type AuthUser } from "../api/auth";
import { AuthContext } from "./AuthContext";

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const restore = useCallback(async () => {
    setLoading(true);
    setError(null);
    try { setUser(await currentUser()); }
    catch { setError("Unable to check your session. Please try again."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => {
    let active = true;
    currentUser().then(value => { if (active) setUser(value); })
      .catch(() => { if (active) setError("Unable to check your session. Please try again."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  async function signOut() {
    await logout();
    setUser(null);
  }
  return <AuthContext.Provider value={{ user, loading, error, signIn: setUser, signOut, restore }}>
    {children}
  </AuthContext.Provider>;
}
