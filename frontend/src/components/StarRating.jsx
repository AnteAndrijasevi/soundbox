import { useState } from 'react';

function Star({ fill }) {
  // fill: 0, 0.5 or 1 — gold star overlaid on a dim base so halves render cleanly
  return (
    <span className="star" style={{ position: 'relative', display: 'inline-block' }}>
      <span className="star dim">★</span>
      {fill > 0 && (
        <span
          style={{
            position: 'absolute',
            left: 0,
            top: 0,
            width: fill === 0.5 ? '50%' : '100%',
            overflow: 'hidden',
            color: 'var(--gold)',
          }}
        >
          ★
        </span>
      )}
    </span>
  );
}

function fillFor(value, i) {
  if (value == null) return 0;
  if (value >= i) return 1;
  if (value >= i - 0.5) return 0.5;
  return 0;
}

/** Read-only star display for a 0.5–5.0 rating. */
export function Stars({ rating }) {
  if (rating == null) return null;
  const value = Number(rating);
  return (
    <span className="stars" title={`${value} / 5`}>
      {[1, 2, 3, 4, 5].map((i) => (
        <Star key={i} fill={fillFor(value, i)} />
      ))}
    </span>
  );
}

/** Half-star rating input: click the left half of a star for .5, right half for full. */
export function StarInput({ value, onChange }) {
  const [hover, setHover] = useState(null);
  const shown = hover ?? value;

  const starValue = (i, e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const isLeftHalf = e.clientX - rect.left < rect.width / 2;
    return i - (isLeftHalf ? 0.5 : 0);
  };

  return (
    <span className="stars input" onMouseLeave={() => setHover(null)} aria-label="Rating">
      {[1, 2, 3, 4, 5].map((i) => (
        <span
          key={i}
          onMouseMove={(e) => setHover(starValue(i, e))}
          onClick={(e) => {
            const v = starValue(i, e);
            onChange(v === value ? null : v); // click the current value to clear
          }}
        >
          <Star fill={fillFor(shown, i)} />
        </span>
      ))}
      <span style={{ fontSize: 13, color: 'var(--ink-soft)', marginLeft: 10, letterSpacing: 0 }}>
        {shown != null ? `${shown} / 5` : 'optional'}
      </span>
    </span>
  );
}
