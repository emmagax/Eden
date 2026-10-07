import { createContext, useContext } from "react";
import type { AuthUser } from "../api/auth";

export type Session = {
  user: AuthUser | null;
  loading: boolean;
  error: string | null;
  signIn: (user: AuthUser) => void;
  signOut: () => Promise<void>;
  restore: () => Promise<void>;
};
export const AuthContext = createContext<Session | null>(null);
export function useAuth() { return useContext(AuthContext); }
