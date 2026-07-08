import { useState } from 'react';
import Modal from './Modal';
import { StarInput } from './StarRating';
import { MOODS, CONTEXTS, parseTracklist } from '../constants';
import { logListen } from '../api';

/**
 * The heart of Soundbox: log a listen in under ten seconds.
 * Everything is optional — one tap on "Log it" is a valid entry.
 */
export default function LogListenModal({ album, onClose, onLogged }) {
  const [rating, setRating] = useState(null);
  const [mood, setMood] = useState(null);
  const [context, setContext] = useState(null);
  const [isFirstListen, setIsFirstListen] = useState(false);
  const [favoriteTrack, setFavoriteTrack] = useState('');
  const [note, setNote] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const tracks = parseTracklist(album.tracklist);

  const submit = async () => {
    setSaving(true);
    setError(null);
    try {
      const { data } = await logListen(album.mbid, {
        rating,
        mood,
        context,
        isFirstListen: isFirstListen || null,
        favoriteTrack: favoriteTrack || null,
        note: note.trim() || null,
      });
      onLogged?.(data);
      onClose();
    } catch (e) {
      setError(e.response?.data?.message ?? 'Could not save your listen. Try again.');
      setSaving(false);
    }
  };

  return (
    <Modal onClose={onClose}>
      <h2>Log a listen</h2>
      <p className="modal-sub">
        {album.title}
        {album.artist ? ` — ${album.artist}` : ''}
      </p>

      <div className="field">
        <label>Rating</label>
        <StarInput value={rating} onChange={setRating} />
      </div>

      <div className="field">
        <label>How did it feel?</label>
        <div className="chip-row">
          {MOODS.map((m) => (
            <button
              key={m.value}
              type="button"
              className={`chip${mood === m.value ? ' selected' : ''}`}
              onClick={() => setMood(mood === m.value ? null : m.value)}
            >
              {m.emoji} {m.label}
            </button>
          ))}
        </div>
      </div>

      <div className="field">
        <label>Where were you?</label>
        <div className="chip-row">
          {CONTEXTS.map((c) => (
            <button
              key={c.value}
              type="button"
              className={`chip${context === c.value ? ' selected' : ''}`}
              onClick={() => setContext(context === c.value ? null : c.value)}
            >
              {c.emoji} {c.label}
            </button>
          ))}
        </div>
      </div>

      <div className="field">
        <span
          className={`toggle${isFirstListen ? ' on' : ''}`}
          onClick={() => setIsFirstListen(!isFirstListen)}
          role="switch"
          aria-checked={isFirstListen}
        >
          <span className="track" />
          First time hearing this album
        </span>
      </div>

      {tracks.length > 0 && (
        <div className="field">
          <label>Favorite track</label>
          <select value={favoriteTrack} onChange={(e) => setFavoriteTrack(e.target.value)}>
            <option value="">—</option>
            {tracks.map((t, i) => (
              <option key={i} value={t.title}>
                {t.title}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="field">
        <label>Note</label>
        <textarea
          rows={3}
          maxLength={500}
          placeholder="A line for your future self…"
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
      </div>

      {error && <p className="error-text">{error}</p>}

      <div className="modal-actions">
        <button className="btn secondary" onClick={onClose} disabled={saving}>
          Cancel
        </button>
        <button className="btn" onClick={submit} disabled={saving}>
          {saving ? 'Saving…' : 'Log it'}
        </button>
      </div>
    </Modal>
  );
}
