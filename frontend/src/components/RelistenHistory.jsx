import { Stars } from './StarRating';
import { moodOf, contextOf, formatDateTime } from '../constants';

/** One end of the comparison: how the album felt on a single listen. */
function Snapshot({ log, label }) {
  const mood = moodOf(log.mood);
  const context = contextOf(log.context);
  return (
    <div className="snapshot">
      <div className="snapshot-label">{label}</div>
      <div className="snapshot-date">{formatDateTime(log.listenedAt)}</div>
      {log.rating != null && (
        <div style={{ margin: '6px 0' }}>
          <Stars rating={log.rating} />
        </div>
      )}
      <div className="chip-row" style={{ margin: '6px 0' }}>
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
        {log.isFirstListen && (
          <span className="chip static" style={{ color: 'var(--accent-deep)', borderColor: 'var(--accent)' }}>
            first listen
          </span>
        )}
      </div>
      {log.favoriteTrack && <div className="snapshot-fav">♪ {log.favoriteTrack}</div>}
      {log.note && <div className="entry-note" style={{ marginTop: 8 }}>{log.note}</div>}
    </div>
  );
}

/**
 * "Then vs Now" — the product's reflective centerpiece. Compares a listener's
 * earliest and most recent logs of an album. Renders nothing until there are at
 * least two listens to compare.
 */
export default function RelistenHistory({ logs }) {
  if (!logs || logs.length < 2) return null;

  const then = logs[0];
  const now = logs[logs.length - 1];
  const between = logs.length - 2;

  return (
    <>
      <div className="divider-label" style={{ marginTop: 36 }}>
        Then vs Now — how this album changed with you
      </div>
      <div className="card then-now">
        <Snapshot log={then} label="Then" />
        <div className="then-now-arrow" aria-hidden="true">→</div>
        <Snapshot log={now} label="Now" />
      </div>
      {between > 0 && (
        <p className="page-sub" style={{ marginTop: 10, textAlign: 'center' }}>
          and {between} listen{between === 1 ? '' : 's'} in between
        </p>
      )}
    </>
  );
}
