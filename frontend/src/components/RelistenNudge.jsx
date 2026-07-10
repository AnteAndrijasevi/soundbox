import { Link } from 'react-router-dom';
import AlbumCover from './AlbumCover';

/**
 * "One year ago" retention nudge — a warm row of the albums you logged around a
 * year ago, inviting a revisit. Renders nothing when there's nothing to nudge about.
 */
export default function RelistenNudge({ nudges }) {
  if (!nudges || nudges.length === 0) return null;

  return (
    <div className="card nudge">
      <div className="nudge-head">
        <span className="nudge-kicker">One year ago</span>
        <span className="nudge-sub">this time last year you were listening to…</span>
      </div>
      <div className="nudge-row">
        {nudges.map((n) => {
          const album = {
            mbid: n.albumMbid,
            title: n.albumTitle,
            coverArtUrl: n.albumCoverArtUrl,
          };
          return (
            <Link key={n.id} to={`/album/${n.albumMbid}`} className="nudge-item" title={n.albumTitle}>
              <AlbumCover album={album} size={250} />
              <span className="nudge-title">{n.albumTitle}</span>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
