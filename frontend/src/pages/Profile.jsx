import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getUser, getUserReviews, getUserLog, getMyLog, getUserLists, toggleFollow, updateBio } from '../api';
import { useAuth } from '../auth/AuthContext';
import ReviewCard from '../components/ReviewCard';
import LogEntry from '../components/LogEntry';
import Pagination from '../components/Pagination';

export default function Profile() {
  const { userId } = useParams();
  const { user: me, refreshUser } = useAuth();
  const isOwn = String(me?.id) === String(userId);

  const [profile, setProfile] = useState(null);
  const [error, setError] = useState(null);
  const [tab, setTab] = useState('log');
  const [followBusy, setFollowBusy] = useState(false);

  const [editingBio, setEditingBio] = useState(false);
  const [bioDraft, setBioDraft] = useState('');

  const [log, setLog] = useState(null);
  const [logPage, setLogPage] = useState(0);
  const [reviews, setReviews] = useState(null);
  const [reviewPage, setReviewPage] = useState(0);
  const [lists, setLists] = useState(null);

  const loadProfile = useCallback(() => {
    getUser(userId)
      .then(({ data }) => setProfile(data))
      .catch(() => setError('Could not load this profile.'));
  }, [userId]);

  useEffect(() => {
    setProfile(null);
    setError(null);
    setTab('log');
    setLog(null);
    setReviews(null);
    setLists(null);
    setLogPage(0);
    setReviewPage(0);
    loadProfile();
  }, [userId, loadProfile]);

  useEffect(() => {
    const fetchLog = isOwn ? getMyLog(logPage) : getUserLog(userId, logPage);
    fetchLog.then(({ data }) => setLog(data)).catch(() => {});
  }, [userId, isOwn, logPage]);

  const reloadReviews = useCallback(() => {
    getUserReviews(userId, reviewPage)
      .then(({ data }) => setReviews(data))
      .catch(() => {});
  }, [userId, reviewPage]);

  useEffect(reloadReviews, [reloadReviews]);

  useEffect(() => {
    getUserLists(userId)
      .then(({ data }) => setLists(data))
      .catch(() => {});
  }, [userId]);

  const handleFollow = async () => {
    setFollowBusy(true);
    try {
      await toggleFollow(userId);
      loadProfile();
    } finally {
      setFollowBusy(false);
    }
  };

  const saveBio = async () => {
    await updateBio(bioDraft.trim());
    setEditingBio(false);
    loadProfile();
    refreshUser();
  };

  if (error) return <div className="empty">{error}</div>;
  if (!profile) return <div className="spinner-wrap">loading profile…</div>;

  return (
    <>
      <div style={{ display: 'flex', gap: 20, alignItems: 'flex-start', marginBottom: 8, flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: 260 }}>
          <h1 className="page-title" style={{ marginBottom: 2 }}>
            {profile.username}
          </h1>
          <p className="page-sub" style={{ margin: 0 }}>
            {profile.followerCount} follower{profile.followerCount === 1 ? '' : 's'} · {profile.followingCount}{' '}
            following · {profile.reviewCount} review{profile.reviewCount === 1 ? '' : 's'}
          </p>

          {!editingBio && (
            <p style={{ fontFamily: 'var(--serif)', fontStyle: 'italic', color: 'var(--ink-soft)', marginTop: 12 }}>
              {profile.bio || (isOwn ? 'No bio yet — say something about your ears.' : '')}
              {isOwn && (
                <button
                  className="btn ghost small"
                  style={{ marginLeft: 8 }}
                  onClick={() => {
                    setBioDraft(profile.bio ?? '');
                    setEditingBio(true);
                  }}
                >
                  Edit
                </button>
              )}
            </p>
          )}
          {editingBio && (
            <div style={{ marginTop: 12, maxWidth: 480 }}>
              <textarea
                rows={3}
                style={{ width: '100%' }}
                value={bioDraft}
                onChange={(e) => setBioDraft(e.target.value)}
                autoFocus
              />
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                <button className="btn small" onClick={saveBio}>
                  Save
                </button>
                <button className="btn secondary small" onClick={() => setEditingBio(false)}>
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>

        {!isOwn && (
          <button className="btn secondary" onClick={handleFollow} disabled={followBusy}>
            Follow / Unfollow
          </button>
        )}
      </div>

      <div className="tabs">
        <button className={`tab${tab === 'log' ? ' active' : ''}`} onClick={() => setTab('log')}>
          Listen log
        </button>
        <button className={`tab${tab === 'reviews' ? ' active' : ''}`} onClick={() => setTab('reviews')}>
          Reviews
        </button>
        <button className={`tab${tab === 'lists' ? ' active' : ''}`} onClick={() => setTab('lists')}>
          Lists
        </button>
      </div>

      {tab === 'log' &&
        (log == null ? (
          <div className="spinner-wrap">loading listen log…</div>
        ) : log.content.length === 0 ? (
          <div className="empty">
            {isOwn ? (
              <>
                Your diary is empty. <Link to="/search" style={{ color: 'var(--accent-deep)' }}>Log your first listen</Link>.
              </>
            ) : (
              'No listens logged yet.'
            )}
          </div>
        ) : (
          <>
            <div className="card">
              {log.content.map((l) => (
                <LogEntry key={l.id} log={l} />
              ))}
            </div>
            <Pagination page={log} onPageChange={setLogPage} />
          </>
        ))}

      {tab === 'reviews' &&
        (reviews == null ? (
          <div className="spinner-wrap">loading reviews…</div>
        ) : reviews.content.length === 0 ? (
          <div className="empty">No reviews yet.</div>
        ) : (
          <>
            <div className="card">
              {reviews.content.map((r) => (
                <ReviewCard key={r.id} review={r} onDeleted={reloadReviews} />
              ))}
            </div>
            <Pagination page={reviews} onPageChange={setReviewPage} />
          </>
        ))}

      {tab === 'lists' &&
        (lists == null ? (
          <div className="spinner-wrap">loading lists…</div>
        ) : lists.length === 0 ? (
          <div className="empty">No lists yet.</div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 16 }}>
            {lists.map((l) => (
              <Link key={l.id} to={`/lists/${l.id}`} className="card" style={{ padding: '16px 18px' }}>
                <h3 style={{ marginBottom: 4 }}>{l.name}</h3>
                {l.description && (
                  <p style={{ margin: '0 0 8px', fontSize: 13.5, color: 'var(--ink-soft)' }}>{l.description}</p>
                )}
                <span className="entry-meta">
                  {l.itemCount} album{l.itemCount === 1 ? '' : 's'}
                </span>
              </Link>
            ))}
          </div>
        ))}
    </>
  );
}
