import {
  createContext,
  useContext,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import { apiFetch, clearToken } from './api';

// ---- Types (mirror backend DTOs) ----

export interface User {
  id: string;
  name: string;
  email: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// ---- Context ----

interface AuthState {
  token: string | null;
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

function loadSession(): { token: string | null; user: User | null } {
  const token = sessionStorage.getItem('admin_token');
  if (!token) return { token: null, user: null };

  try {
    const userJson = sessionStorage.getItem('admin_user');
    if (!userJson) return { token: null, user: null };
    const user = JSON.parse(userJson) as User;
    if (user.role !== 'ADMIN') return { token: null, user: null };
    return { token, user };
  } catch {
    return { token: null, user: null };
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => loadSession().token);
  const [user, setUser] = useState<User | null>(() => loadSession().user);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = useCallback(async (email: string, password: string) => {
    setLoading(true);
    setError(null);

    try {
      const res = await apiFetch<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password } satisfies LoginRequest),
      });

      if (res.user.role !== 'ADMIN') {
        // Non-admin login: clear everything — per spec scenario
        sessionStorage.removeItem('admin_token');
        sessionStorage.removeItem('admin_user');
        setToken(null);
        setUser(null);
        setError('Access denied — ADMIN role required');
        return;
      }

      sessionStorage.setItem('admin_token', res.token);
      sessionStorage.setItem('admin_user', JSON.stringify(res.user));
      setToken(res.token);
      setUser(res.user);
      setError(null);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Login failed. Please try again.';
      setError(message);
      sessionStorage.removeItem('admin_token');
      sessionStorage.removeItem('admin_user');
      setToken(null);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearToken();
    sessionStorage.removeItem('admin_user');
    setToken(null);
    setUser(null);
    setError(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, user, loading, error, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
