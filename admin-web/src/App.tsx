import { Routes, Route, Navigate, Link } from 'react-router-dom';
import { useAuth } from './lib/auth';
import Login from './pages/Login';
import Users from './pages/Users';
import Courses from './pages/Courses';

function AuthGuard({ children }: { children: React.ReactNode }) {
  const { token, user } = useAuth();

  if (!token || !user || user.role !== 'ADMIN') {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

function NavBar() {
  const { logout } = useAuth();

  return (
    <nav className="nav-bar">
      <Link to="/users" className="nav-link">
        Users
      </Link>
      <Link to="/courses" className="nav-link">
        Courses
      </Link>
      <button className="logout-btn" onClick={logout}>
        Log out
      </button>
    </nav>
  );
}

export default function App() {
  const { token, user } = useAuth();

  return (
    <div className="app-shell">
      {token && user?.role === 'ADMIN' && <NavBar />}
      <main className="main-content">
        <Routes>
          <Route
            path="/login"
            element={
              token && user?.role === 'ADMIN' ? (
                <Navigate to="/users" replace />
              ) : (
                <Login />
              )
            }
          />
          <Route
            path="/users"
            element={
              <AuthGuard>
                <Users />
              </AuthGuard>
            }
          />
          <Route
            path="/courses"
            element={
              <AuthGuard>
                <Courses />
              </AuthGuard>
            }
          />
          <Route path="*" element={<Navigate to="/users" replace />} />
        </Routes>
      </main>
    </div>
  );
}
