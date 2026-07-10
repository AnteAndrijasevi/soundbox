import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { getUnreadCount } from '../api';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const refresh = () =>
      getUnreadCount()
        .then(({ data }) => !cancelled && setUnread(data.count))
        .catch(() => {});
    refresh();
    const id = setInterval(refresh, 20000); // light polling
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [location.pathname]); // re-check after navigating (e.g. away from notifications)

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
          <NavLink
            to="/notifications"
            className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
          >
            Notifications
            {unread > 0 && <span className="nav-badge">{unread}</span>}
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
