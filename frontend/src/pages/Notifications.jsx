import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getNotifications, markNotificationsRead } from '../api';
import { formatDateTime } from '../constants';

export default function Notifications() {
  const [page, setPage] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getNotifications()
      .then(({ data }) => setPage(data))
      .catch(() => setError('Could not load your notifications.'))
      // mark everything read once viewed
      .finally(() => markNotificationsRead().catch(() => {}));
  }, []);

  return (
    <>
      <h1 className="page-title">Notifications</h1>
      <p className="page-sub">When someone you follow logs a listen, it shows up here.</p>

      {error && <p className="error-text">{error}</p>}
      {!page && !error && <div className="spinner-wrap">loading…</div>}

      {page && page.content.length === 0 && (
        <div className="empty">
          Nothing yet. Follow some listeners from their profiles and you’ll see what they’re playing.
        </div>
      )}

      {page && page.content.length > 0 && (
        <div className="card">
          {page.content.map((n) => (
            <div key={n.id} className="entry" style={{ alignItems: 'center' }}>
              <div className="entry-body">
                <div style={{ fontSize: 14.5 }}>
                  {n.albumMbid ? (
                    <>
                      <strong>{n.actorUsername}</strong> logged{' '}
                      <Link to={`/album/${n.albumMbid}`} style={{ color: 'var(--accent-deep)', fontWeight: 600 }}>
                        {n.albumTitle}
                      </Link>
                    </>
                  ) : (
                    n.message
                  )}
                </div>
                <div className="entry-meta">{formatDateTime(n.createdAt)}</div>
              </div>
              {!n.read && (
                <span
                  aria-label="unread"
                  style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--accent)', flexShrink: 0 }}
                />
              )}
            </div>
          ))}
        </div>
      )}
    </>
  );
}
