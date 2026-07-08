import { Link } from 'react-router-dom';
import { useState } from 'react';
import { Stars } from './StarRating';
import { formatDateTime } from '../constants';
import { toggleLike, deleteReview } from '../api';
import { useAuth } from '../auth/AuthContext';

/**
 * A single review. Shows album context (title link) when `showAlbum` is set —
 * used in the feed and on profiles; hidden on the album page itself.
 */
export default function ReviewCard({ review, showAlbum = true, onDeleted }) {
  const { user } = useAuth();
  const [likeCount, setLikeCount] = useState(review.likeCount);
  const [liked, setLiked] = useState(false); // session-local; API is a blind toggle
  const isOwn = user?.username === review.username;

  const handleLike = async () => {
    // optimistic; the API toggles and doesn't report state
    setLiked(!liked);
    setLikeCount((c) => c + (liked ? -1 : 1));
    try {
      await toggleLike(review.id);
    } catch {
      setLiked(liked);
      setLikeCount(review.likeCount);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete this review?')) return;
    await deleteReview(review.id);
    onDeleted?.(review.id);
  };

  return (
    <div className="entry">
      <div className="entry-body">
        <div className="entry-head">
          {showAlbum && (
            <Link to={`/album/${review.albumMbid}`} className="entry-title">
              {review.albumTitle}
            </Link>
          )}
          <Stars rating={review.rating} />
          <span className="entry-meta">{formatDateTime(review.createdAt)}</span>
        </div>
        <div className="byline">
          review by{' '}
          {review.userId != null ? (
            <Link to={`/users/${review.userId}`}>{review.username}</Link>
          ) : (
            review.username
          )}
        </div>
        {review.text && <p className="entry-text">{review.text}</p>}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <button className="btn ghost small" onClick={handleLike}>
            {liked ? '♥' : '♡'} {likeCount}
          </button>
          {isOwn && (
            <button className="btn ghost small" onClick={handleDelete}>
              Delete
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
