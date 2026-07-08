import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <>
      <nav className="nav">
        <div className="nav-inner">
          <NavLink to="/" className="brand">
            soundbox
          </NavLink>
          <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Feed
          </NavLink>
          <NavLink to="/search" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Search
          </NavLink>
          <NavLink to="/lists" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Lists
          </NavLink>
          <div className="nav-spacer" />
          <NavLink
            to={`/users/${user.id}`}
            className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
          >
            {user.username}
          </NavLink>
          <button className="btn ghost small" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </nav>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}
