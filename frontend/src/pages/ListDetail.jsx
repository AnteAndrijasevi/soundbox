import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deleteList, getListDetail, removeAlbumFromList } from '../api';
import { useAuth } from '../auth/AuthContext';
import AlbumCover from '../components/AlbumCover';

export default function ListDetail() {
  const { listId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [list, setList] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    getListDetail(listId)
      .then(({ data }) => setList(data))
      .catch(() => setError('Could not load this list.'));
  }, [listId]);

  useEffect(load, [load]);

  if (error) return <div className="empty">{error}</div>;
  if (!list) return <div className="spinner-wrap">loading list…</div>;

  const isOwn = user?.username === list.username;

  const handleRemove = async (mbid) => {
    await removeAlbumFromList(listId, mbid);
    load();
  };

  const handleDelete = async () => {
    if (!window.confirm(`Delete the list “${list.name}”? This cannot be undone.`)) return;
    await deleteList(listId);
    navigate('/lists');
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 16, flexWrap: 'wrap' }}>
        <h1 className="page-title" style={{ flex: 1, marginBottom: 0 }}>
          {list.name}
        </h1>
        {isOwn && (
          <button className="btn danger small" onClick={handleDelete}>
            Delete list
          </button>
        )}
      </div>
      <p className="page-sub">
        a list by {list.username}
        {list.description ? ` — ${list.description}` : ''}
        {!list.isPublic && ' · private'}
      </p>

      {list.items.length === 0 ? (
        <div className="empty">
          Empty shelf. <Link to="/search" style={{ color: 'var(--accent-deep)' }}>Find albums</Link> to add from their
          pages.
        </div>
      ) : (
        <div className="album-grid">
          {list.items.map((item) => (
            <div key={item.albumMbid} className="album-card">
              <Link to={`/album/${item.albumMbid}`}>
                <AlbumCover album={{ mbid: item.albumMbid, title: item.albumTitle, coverArtUrl: item.coverArtUrl }} />
              </Link>
              <div>
                <Link to={`/album/${item.albumMbid}`} className="title" style={{ display: 'block' }}>
                  {item.albumTitle}
                </Link>
                <div className="artist">{item.artist}</div>
                {item.note && (
                  <div style={{ fontSize: 12.5, color: 'var(--ink-faint)', fontStyle: 'italic', marginTop: 2 }}>
                    {item.note}
                  </div>
                )}
                {isOwn && (
                  <button className="btn ghost small" style={{ marginTop: 4 }} onClick={() => handleRemove(item.albumMbid)}>
                    Remove
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
