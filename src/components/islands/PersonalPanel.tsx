import { useEffect, useState } from 'react';

interface TopicInfo {
  id: string;
  title: string;
  url: string;
  track: string;
}
interface TrackInfo {
  id: string;
  title: string;
  url: string;
  total: number;
}
interface Props {
  topics: TopicInfo[];
  tracks: TrackInfo[];
  mode: 'home' | 'progress';
}

const PREFIX = 'efg:';
function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw == null ? fallback : (JSON.parse(raw) as T);
  } catch {
    return fallback;
  }
}

/** Client-only view of what this browser has stored: recent topics, bookmarks, completion. */
export default function PersonalPanel({ topics, tracks, mode }: Props) {
  const [state, setState] = useState<{
    progress: Record<string, number>;
    bookmarks: string[];
    recent: { id: string; at: number }[];
  } | null>(null);

  useEffect(() => {
    const load = () =>
      setState({
        progress: read('progress', {}),
        bookmarks: read('bookmarks', []),
        recent: read('recent', []),
      });
    load();
    window.addEventListener('storage', load);
    return () => window.removeEventListener('storage', load);
  }, []);

  if (!state) return null;
  const byId = new Map(topics.map((t) => [t.id, t]));
  const recent = state.recent.map((r) => byId.get(r.id)).filter((t): t is TopicInfo => !!t);
  const bookmarks = state.bookmarks.map((id) => byId.get(id)).filter((t): t is TopicInfo => !!t);
  const done = Object.keys(state.progress).filter((id) => byId.has(id));

  if (mode === 'home') {
    if (!recent.length && !bookmarks.length) return null;
    return (
      <div className="pp-home">
        {recent.length > 0 && (
          <section>
            <h2 className="eyebrow">Continue reading</h2>
            <ul>
              {recent.slice(0, 5).map((t) => (
                <li key={t.id}>
                  <a href={t.url}>{t.title}</a>
                </li>
              ))}
            </ul>
          </section>
        )}
        {bookmarks.length > 0 && (
          <section>
            <h2 className="eyebrow">Saved</h2>
            <ul>
              {bookmarks.slice(0, 5).map((t) => (
                <li key={t.id}>
                  <a href={t.url}>{t.title}</a>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    );
  }

  const exportData = () => {
    const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'field-guide-progress.json';
    a.click();
    URL.revokeObjectURL(a.href);
  };

  return (
    <div className="pp-progress">
      <p className="pp-total">
        <strong>{done.length}</strong> of {topics.length} topics completed
      </p>
      <ul className="pp-tracks">
        {tracks.map((tr) => {
          const n = done.filter((id) => id.startsWith(`${tr.id}/`)).length;
          const pct = tr.total ? Math.round((n / tr.total) * 100) : 0;
          return (
            <li key={tr.id}>
              <a href={tr.url}>{tr.title}</a>
              <span className="pp-bar" aria-hidden="true">
                <span style={{ width: `${pct}%` }} />
              </span>
              <span className="pp-num">
                {n}/{tr.total}
              </span>
            </li>
          );
        })}
      </ul>
      <h2>Saved topics</h2>
      {bookmarks.length ? (
        <ul>
          {bookmarks.map((t) => (
            <li key={t.id}>
              <a href={t.url}>{t.title}</a>
            </li>
          ))}
        </ul>
      ) : (
        <p className="pp-muted">Nothing saved yet. Use “Save” on any topic.</p>
      )}
      <p className="pp-muted">
        Progress is stored only in this browser.{' '}
        <button type="button" className="btn" onClick={exportData}>
          Export as JSON
        </button>
      </p>
    </div>
  );
}
