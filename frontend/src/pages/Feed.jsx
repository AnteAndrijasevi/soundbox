import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getFeed } from '../api';
import ReviewCard from '../components/ReviewCard';
import Pagination from '../components/Pagination';

export default function Feed() {
  const [page, setPage] = useState(0);
  const [feed, setFeed] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getFeed(page)
      .then(({ data }) => setFeed(data))
      .catch(() => setError('Could not load your feed.'));
  }, [page]);

  return (
    <>
      <h1 className="page-title">Your feed</h1>
      <p className="page-sub">The latest reviews from people you follow.</p>

      {error && <p className="error-text">{error}</p>}
      {!feed && !error && <div className="spinner-wrap">loading your feed…</div>}

      {feed && feed.content.length === 0 && (
        <div className="empty">
          Quiet in here. <Link to="/search" style={{ color: 'var(--accent-deep)' }}>Find an album</Link> to review, or
          follow other listeners from their profiles.
        </div>
      )}

      {feed && feed.content.length > 0 && (
        <>
          <div className="card">
            {feed.content.map((r) => (
              <ReviewCard key={r.id} review={r} />
            ))}
          </div>
          <Pagination page={feed} onPageChange={setPage} />
        </>
      )}
    </>
  );
}
