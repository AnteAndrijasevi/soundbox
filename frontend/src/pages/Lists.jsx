import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { createList, getUserLists } from '../api';
import { useAuth } from '../auth/AuthContext';
import Modal from '../components/Modal';

export default function Lists() {
  const { user } = useAuth();
  const [lists, setLists] = useState(null);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isPublic, setIsPublic] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    getUserLists(user.id)
      .then(({ data }) => setLists(data))
      .catch(() => setError('Could not load your lists.'));
  }, [user.id]);

  useEffect(load, [load]);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      await createList({ name: name.trim(), description: description.trim() || null, isPublic });
      setCreating(false);
      setName('');
      setDescription('');
      setIsPublic(true);
      load();
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not create the list.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
        <h1 className="page-title" style={{ flex: 1 }}>
          Your lists
        </h1>
        <button className="btn" onClick={() => setCreating(true)}>
          + New list
        </button>
      </div>
      <p className="page-sub">Albums, curated your way.</p>

      {error && <p className="error-text">{error}</p>}
      {lists == null && !error && <div className="spinner-wrap">loading lists…</div>}

      {lists?.length === 0 && (
        <div className="empty">No lists yet. Start one — “Albums that raised me” is a classic.</div>
      )}

      {lists?.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
          {lists.map((l) => (
            <Link key={l.id} to={`/lists/${l.id}`} className="card" style={{ padding: '18px 20px' }}>
              <h3 style={{ marginBottom: 4 }}>{l.name}</h3>
              {l.description && (
                <p style={{ margin: '0 0 10px', fontSize: 13.5, color: 'var(--ink-soft)' }}>{l.description}</p>
              )}
              <span className="entry-meta">
                {l.itemCount} album{l.itemCount === 1 ? '' : 's'}
                {!l.isPublic && ' · private'}
              </span>
            </Link>
          ))}
        </div>
      )}

      {creating && (
        <Modal onClose={() => setCreating(false)}>
          <h2>New list</h2>
          <p className="modal-sub">A shelf for a feeling, a year, a scene.</p>
          <form onSubmit={submit}>
            <div className="field">
              <label>Name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} autoFocus required />
            </div>
            <div className="field">
              <label>Description</label>
              <textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
            <div className="field">
              <span
                className={`toggle${isPublic ? ' on' : ''}`}
                onClick={() => setIsPublic(!isPublic)}
                role="switch"
                aria-checked={isPublic}
              >
                <span className="track" />
                Visible to others
              </span>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn secondary" onClick={() => setCreating(false)}>
                Cancel
              </button>
              <button type="submit" className="btn" disabled={busy || !name.trim()}>
                {busy ? 'Creating…' : 'Create list'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
