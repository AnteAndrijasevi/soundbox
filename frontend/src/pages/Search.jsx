import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { searchAlbums } from '../api';
import AlbumCover from '../components/AlbumCover';

export default function Search() {
  const [params, setParams] = useSearchParams();
  const query = params.get('q') ?? '';
  const [input, setInput] = useState(query);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!query) {
      setResults(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    searchAlbums(query)
      .then(({ data }) => {
        if (!cancelled) setResults(data);
      })
      .catch(() => {
        if (!cancelled) setError('Search failed — MusicBrainz may be busy. Try again in a moment.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [query]);

  const submit = (e) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (trimmed) setParams({ q: trimmed });
  };

  return (
    <>
      <h1 className="page-title">Find an album</h1>
      <p className="page-sub">Search the MusicBrainz catalogue and start logging.</p>
      <form onSubmit={submit} style={{ display: 'flex', gap: 10, marginBottom: 28 }}>
        <input
          style={{ flex: 1, fontSize: 16, padding: '12px 16px' }}
          placeholder="Album or artist — try “In Rainbows”"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          autoFocus
        />
        <button className="btn" type="submit" disabled={loading}>
          Search
        </button>
      </form>

      {loading && <div className="spinner-wrap">searching…</div>}
      {error && <p className="error-text">{error}</p>}

      {!loading && results && results.length === 0 && (
        <div className="empty">Nothing found for “{query}”. Try the artist’s name too.</div>
      )}

      {!loading && results && results.length > 0 && (
        <div className="album-grid">
          {results.map((album) => (
            <Link key={album.mbid} to={`/album/${album.mbid}`} className="album-card">
              <AlbumCover album={album} size={250} />
              <div>
                <div className="title">{album.title}</div>
                <div className="artist">{album.artist}</div>
                {album.releaseDate && <div className="date">{album.releaseDate.slice(0, 4)}</div>}
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
