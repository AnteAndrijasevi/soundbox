import { Link } from 'react-router-dom';
import AlbumCover from './AlbumCover';
import { Stars } from './StarRating';
import { moodOf, contextOf, formatDateTime } from '../constants';

/** One diary entry in a listen log. */
export default function LogEntry({ log }) {
  const mood = moodOf(log.mood);
  const context = contextOf(log.context);
  const album = {
    mbid: log.albumMbid,
    title: log.albumTitle,
    coverArtUrl: log.albumCoverArtUrl,
  };

  return (
    <div className="entry">
      <div className="cover-wrap">
        <Link to={`/album/${log.albumMbid}`}>
          <AlbumCover album={album} size={250} />
        </Link>
      </div>
      <div className="entry-body">
        <div className="entry-head">
          <Link to={`/album/${log.albumMbid}`} className="entry-title">
            {log.albumTitle}
          </Link>
          {log.isFirstListen && (
            <span className="chip static" style={{ color: 'var(--accent-deep)', borderColor: 'var(--accent)' }}>
              first listen
            </span>
          )}
          <span className="entry-meta">{formatDateTime(log.listenedAt)}</span>
        </div>
        {log.artist && <div className="byline">{log.artist}</div>}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginTop: 6 }}>
          <Stars rating={log.rating} />
          {mood && (
            <span className="chip static">
              {mood.emoji} {mood.label}
            </span>
          )}
          {context && (
            <span className="chip static">
              {context.emoji} {context.label}
            </span>
          )}
          {log.favoriteTrack && (
            <span className="chip static" title="Favorite track">
              ♪ {log.favoriteTrack}
            </span>
          )}
        </div>
        {log.note && <div className="entry-note">{log.note}</div>}
      </div>
    </div>
  );
}
