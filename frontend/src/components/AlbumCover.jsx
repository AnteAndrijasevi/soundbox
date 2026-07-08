import { useState } from 'react';

/**
 * Album cover with graceful fallbacks: prefers the iTunes artwork, then the
 * Cover Art Archive URL stored on the album, then CAA's by-mbid redirect
 * (works for search results that were never cached), then a placeholder.
 */
export default function AlbumCover({ album, size = 250, className = 'cover' }) {
  const candidates = [
    album?.artworkUrl,
    album?.coverArtUrl,
    album?.mbid ? `https://coverartarchive.org/release/${album.mbid}/front-${size}` : null,
  ].filter(Boolean);

  const [index, setIndex] = useState(0);

  if (index >= candidates.length) {
    return (
      <div className="cover-placeholder" aria-label={album?.title}>
        ♪
      </div>
    );
  }

  return (
    <img
      className={className}
      src={candidates[index]}
      alt={album?.title ?? 'Album cover'}
      loading="lazy"
      onError={() => setIndex((i) => i + 1)}
    />
  );
}
