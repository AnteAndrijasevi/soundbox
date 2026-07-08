import { useEffect, useState } from 'react';
import Modal from './Modal';
import { getUserLists, addAlbumToList, createList } from '../api';
import { useAuth } from '../auth/AuthContext';

export default function AddToListModal({ album, onClose, onAdded }) {
  const { user } = useAuth();
  const [lists, setLists] = useState(null);
  const [selected, setSelected] = useState(null);
  const [newListName, setNewListName] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    getUserLists(user.id)
      .then(({ data }) => setLists(data))
      .catch(() => setError('Could not load your lists.'));
  }, [user.id]);

  const submit = async () => {
    setSaving(true);
    setError(null);
    try {
      let listId = selected;
      if (!listId) {
        if (!newListName.trim()) {
          setError('Pick a list or name a new one.');
          setSaving(false);
          return;
        }
        const { data } = await createList({ name: newListName.trim(), description: null, isPublic: true });
        listId = data.id;
      }
      await addAlbumToList(listId, album.mbid);
      onAdded?.(listId);
      onClose();
    } catch (e) {
      setError(e.response?.data?.message ?? 'Could not add the album to the list.');
      setSaving(false);
    }
  };

  return (
    <Modal onClose={onClose}>
      <h2>Add to list</h2>
      <p className="modal-sub">
        {album.title}
        {album.artist ? ` — ${album.artist}` : ''}
      </p>

      {lists === null && !error ? (
        <div className="spinner-wrap">loading your lists…</div>
      ) : (
        <>
          {lists?.length > 0 && (
            <div className="field">
              <label>Your lists</label>
              <div className="chip-row">
                {lists.map((l) => (
                  <button
                    key={l.id}
                    type="button"
                    className={`chip${selected === l.id ? ' selected' : ''}`}
                    onClick={() => {
                      setSelected(selected === l.id ? null : l.id);
                      setNewListName('');
                    }}
                  >
                    {l.name} · {l.itemCount}
                  </button>
                ))}
              </div>
            </div>
          )}
          <div className="field">
            <label>{lists?.length ? 'Or start a new one' : 'Start a list'}</label>
            <input
              placeholder="e.g. Rainy Sunday albums"
              value={newListName}
              onChange={(e) => {
                setNewListName(e.target.value);
                setSelected(null);
              }}
            />
          </div>
        </>
      )}

      {error && <p className="error-text">{error}</p>}

      <div className="modal-actions">
        <button className="btn secondary" onClick={onClose} disabled={saving}>
          Cancel
        </button>
        <button className="btn" onClick={submit} disabled={saving || lists === null}>
          {saving ? 'Adding…' : 'Add album'}
        </button>
      </div>
    </Modal>
  );
}
