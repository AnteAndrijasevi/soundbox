export const MOODS = [
  { value: 'HAPPY', label: 'Happy', emoji: '😊' },
  { value: 'MELANCHOLIC', label: 'Melancholic', emoji: '🌧️' },
  { value: 'NOSTALGIC', label: 'Nostalgic', emoji: '📼' },
  { value: 'CALM', label: 'Calm', emoji: '🌿' },
  { value: 'ANXIOUS', label: 'Anxious', emoji: '😬' },
  { value: 'EUPHORIC', label: 'Euphoric', emoji: '✨' },
  { value: 'SAD', label: 'Sad', emoji: '💧' },
  { value: 'ENERGETIC', label: 'Energetic', emoji: '⚡' },
];

export const CONTEXTS = [
  { value: 'DRIVING', label: 'Driving', emoji: '🚗' },
  { value: 'WALKING', label: 'Walking', emoji: '🚶' },
  { value: 'MORNING', label: 'Morning', emoji: '🌅' },
  { value: 'NIGHT', label: 'Night', emoji: '🌙' },
  { value: 'WORKING', label: 'Working', emoji: '💼' },
  { value: 'STUDYING', label: 'Studying', emoji: '📚' },
  { value: 'HEARTBREAK', label: 'Heartbreak', emoji: '💔' },
  { value: 'PARTYING', label: 'Partying', emoji: '🎉' },
  { value: 'RELAXING', label: 'Relaxing', emoji: '🛋️' },
];

export const moodOf = (value) => MOODS.find((m) => m.value === value);
export const contextOf = (value) => CONTEXTS.find((c) => c.value === value);

/** The album tracklist is stored as serialized MusicBrainz media JSON. */
export function parseTracklist(tracklistJson) {
  if (!tracklistJson) return [];
  try {
    const media = JSON.parse(tracklistJson);
    if (!Array.isArray(media)) return [];
    return media.flatMap((m) => m?.tracks ?? []).filter((t) => t?.title);
  } catch {
    return [];
  }
}

export function formatTrackLength(ms) {
  if (!ms) return '';
  const total = Math.round(ms / 1000);
  const min = Math.floor(total / 60);
  const sec = String(total % 60).padStart(2, '0');
  return `${min}:${sec}`;
}

export function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function formatDateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}
