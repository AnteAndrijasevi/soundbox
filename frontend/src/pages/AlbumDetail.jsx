import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getAlbum, getAlbumReviews } from '../api';
import AlbumCover from '../components/AlbumCover';
import ReviewCard from '../components/ReviewCard';
import Pagination from '../components/Pagination';
import LogListenModal from '../components/LogListenModal';
import ReviewModal from '../components/ReviewModal';
import AddToListModal from '../components/AddToListModal';
import { parseTracklist, formatTrackLength } from '../constants';

export default function AlbumDetail() {
  const { mbid } = useParams();
  const [album, setAlbum] = useState(null);
  const [error, setError] = useState(null);
  const [reviews, setReviews] = useState(null);
  const [reviewPage, setReviewPage] = useState(0);
  const [modal, setModal] = useState(null); // 'log' | 'review' | 'list'
  const [toast, setToast] = useState(null);

  useEffect(() => {
    setAlbum(null);
    setError(null);
    getAlbum(mbid)
      .then(({ data }) => setAlbum(data))
      .catch(() => setError('Could not load this album.'));
  }, [mbid]);

  const loadReviews = useCallback(() => {
    getAlbumReviews(mbid, reviewPage)
      .then(({ data }) => setReviews(data))
      .catch(() => {});
  }, [mbid, reviewPage]);

  useEffect(loadReviews, [loadReviews]);

  const flash = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  };

  if (error) return <div className="empty">{error}</div>;
  if (!album) return <div className="spinner-wrap">loading album…</div>;

  const tracks = parseTracklist(album.tracklist);

  return (
    <>
      <div style={{ display: 'flex', gap: 32, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div style={{ flex: '0 0 260px', maxWidth: 260 }}>
          <AlbumCover album={album} size={500} />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 18 }}>
            <button className="btn" onClick={() => setModal('log')}>
              ⏺ Log listen
            </button>
            <button className="btn secondary" onClick={() => setModal('review')}>
              ✎ Review
            </button>
            <button className="btn secondary" onClick={() => setModal('list')}>
              + Add to list
            </button>
          </div>
        </div>

        <div style={{ flex: 1, minWidth: 280 }}>
          <h1 className="page-title" style={{ marginBottom: 2 }}>
            {album.title}
          </h1>
          <p className="page-sub" style={{ marginBottom: 12 }}>
            {album.artist}
            {album.releaseDate ? ` · ${album.releaseDate.slice(0, 4)}` : ''}
          </p>

          {album.genres?.length > 0 && (
            <div className="chip-row" style={{ marginBottom: 20 }}>
              {album.genres.map((g) => (
                <span key={g} className="chip static">
                  {g}
                </span>
              ))}
            </div>
          )}

          {tracks.length > 0 && (
            <div className="card" style={{ padding: '6px 0' }}>
              {tracks.map((t, i) => (
                <div
                  key={i}
                  style={{
                    display: 'flex',
                    gap: 12,
                    padding: '7px 18px',
                    fontSize: 14,
                    borderTop: i === 0 ? 'none' : '1px solid var(--line)',
                  }}
                >
                  <span style={{ color: 'var(--ink-faint)', width: 22, textAlign: 'right' }}>
                    {t.number ?? i + 1}
                  </span>
                  <span style={{ flex: 1 }}>{t.title}</span>
                  <span style={{ color: 'var(--ink-faint)' }}>{formatTrackLength(t.length)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="divider-label" style={{ marginTop: 40 }}>
        Reviews
      </div>
      {reviews == null ? (
        <div className="spinner-wrap">loading reviews…</div>
      ) : reviews.content.length === 0 ? (
        <div className="empty">No reviews yet — be the first to write one.</div>
      ) : (
        <>
          <div className="card">
            {reviews.content.map((r) => (
              <ReviewCard key={r.id} review={r} showAlbum={false} onDeleted={loadReviews} />
            ))}
          </div>
          <Pagination page={reviews} onPageChange={setReviewPage} />
        </>
      )}

      {modal === 'log' && (
        <LogListenModal album={album} onClose={() => setModal(null)} onLogged={() => flash('Listen logged ✓')} />
      )}
      {modal === 'review' && (
        <ReviewModal
          album={album}
          onClose={() => setModal(null)}
          onSaved={() => {
            flash('Review published ✓');
            loadReviews();
          }}
        />
      )}
      {modal === 'list' && (
        <AddToListModal album={album} onClose={() => setModal(null)} onAdded={() => flash('Added to list ✓')} />
      )}

      {toast && (
        <div
          style={{
            position: 'fixed',
            bottom: 28,
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'var(--ink)',
            color: 'var(--paper)',
            padding: '10px 22px',
            borderRadius: 999,
            fontSize: 14,
            boxShadow: 'var(--shadow-lift)',
            zIndex: 200,
          }}
        >
          {toast}
        </div>
      )}
    </>
  );
}
