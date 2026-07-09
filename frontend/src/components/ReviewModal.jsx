import { useState } from 'react';
import Modal from './Modal';
import { StarInput } from './StarRating';
import { apiError, submitReview } from '../api';

export default function ReviewModal({ album, onClose, onSaved }) {
  const [rating, setRating] = useState(null);
  const [text, setText] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const submit = async () => {
    if (rating == null) {
      setError('A rating is required (0.5–5 stars).');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const { data } = await submitReview(album.mbid, { rating, text: text.trim() || null });
      onSaved?.(data);
      onClose();
    } catch (e) {
      setError(apiError(e, 'Could not save your review. Try again.'));
      setSaving(false);
    }
  };

  return (
    <Modal onClose={onClose}>
      <h2>Review</h2>
      <p className="modal-sub">
        {album.title}
        {album.artist ? ` — ${album.artist}` : ''} · writing again replaces your previous review
      </p>

      <div className="field">
        <label>Rating</label>
        <StarInput value={rating} onChange={setRating} />
      </div>

      <div className="field">
        <label>Your thoughts</label>
        <textarea
          rows={6}
          placeholder="What did this album do to you?"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
      </div>

      {error && <p className="error-text">{error}</p>}

      <div className="modal-actions">
        <button className="btn secondary" onClick={onClose} disabled={saving}>
          Cancel
        </button>
        <button className="btn" onClick={submit} disabled={saving}>
          {saving ? 'Saving…' : 'Publish'}
        </button>
      </div>
    </Modal>
  );
}
