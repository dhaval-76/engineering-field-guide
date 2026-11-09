import { useEffect, useMemo, useState } from 'react';
import {
  loadHighlights,
  saveHighlights,
  type Highlight,
  type HighlightMap,
} from '~/lib/highlights';
import { broadcast, keys, NOTE_MAX_CHARS, read, writeStrict, type NotesMap } from '~/lib/storage';
import './notes.css';

interface Props {
  topics: { id: string; title: string; url: string }[];
  dataUrl: string;
}
interface Group {
  topic: string;
  note?: { text: string; at: number } | undefined;
  highlights: [string, Highlight][];
  latest: number;
}

const date = (t: number) =>
  new Date(t).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });

/** Every note and highlight in this browser, grouped by topic, latest activity first. */
export default function Notes({ topics, dataUrl }: Props) {
  const [notes, setNotes] = useState<NotesMap | null>(null);
  const [highlights, setHighlights] = useState<HighlightMap>({});
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const load = () => {
      setNotes(read<NotesMap>(keys.notes, {}));
      setHighlights(loadHighlights());
    };
    load();
    addEventListener('storage', load);
    return () => removeEventListener('storage', load);
  }, []);

  const byId = useMemo(() => new Map(topics.map((t) => [t.id, t])), [topics]);
  if (!notes) return null;

  const attempt = (write: () => void) => {
    try {
      write();
      setError('');
      return true;
    } catch {
      setError('Could not save: this browser is blocking storage or is out of space.');
      return false;
    }
  };
  const putHighlights = (next: HighlightMap) => {
    if (attempt(() => saveHighlights(next))) setHighlights(next);
  };
  const saveNote = (id: string, h: Highlight) => {
    const note = draft.trim();
    const next: Highlight = { ...h, at: Date.now() };
    if (note) next.note = note;
    else delete next.note;
    putHighlights({ ...highlights, [id]: next });
    setEditing(null);
  };
  const removeHighlight = (id: string, h: Highlight) => {
    if (h.note && !confirm('Remove this highlight and its note?')) return;
    const rest = { ...highlights };
    delete rest[id];
    putHighlights(rest);
  };
  const removeTopicNote = (topic: string) => {
    if (!confirm('Delete this note?')) return;
    const rest = { ...notes };
    delete rest[topic];
    const saved = attempt(() => {
      writeStrict(keys.notes, rest);
      broadcast(keys.notes);
    });
    if (saved) setNotes(rest);
  };

  const groups = new Map<string, Group>();
  const group = (topic: string) => {
    const g = groups.get(topic) ?? { topic, highlights: [], latest: 0 };
    groups.set(topic, g);
    return g;
  };
  for (const [topic, note] of Object.entries(notes)) {
    const g = group(topic);
    g.note = note;
    g.latest = Math.max(g.latest, note.at);
  }
  for (const entry of Object.entries(highlights)) {
    const g = group(entry[1].topic);
    g.highlights.push(entry);
    g.latest = Math.max(g.latest, entry[1].at);
  }

  if (!groups.size)
    return (
      <p className="nt-muted">
        Nothing yet. Select text on any topic to highlight it or add a note, or write in the “Your
        notes” box at the end of a topic.
      </p>
    );

  const q = query.trim().toLowerCase();
  const titleOf = (topic: string) => byId.get(topic)?.title ?? topic;
  const matches = (text?: string) => !!text && text.toLowerCase().includes(q);
  const shown = [...groups.values()]
    .sort((a, b) => b.latest - a.latest)
    .map((g) => {
      if (!q || matches(titleOf(g.topic))) return g;
      return {
        ...g,
        note: matches(g.note?.text) ? g.note : undefined,
        highlights: g.highlights.filter(([, h]) => matches(h.quote) || matches(h.note)),
      };
    })
    .filter((g) => g.note || g.highlights.length);
  const total = Object.keys(notes).length + Object.keys(highlights).length;

  return (
    <div className="nt">
      <input
        type="search"
        className="nt-filter"
        placeholder="Filter notes and highlights"
        aria-label="Filter notes and highlights"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <p className="nt-muted">
        {total} {total === 1 ? 'item' : 'items'} across {groups.size}{' '}
        {groups.size === 1 ? 'topic' : 'topics'}. Stored only in this browser;{' '}
        <a href={dataUrl}>back them up</a>.
      </p>
      {error && (
        <p className="nt-error" role="alert">
          {error}
        </p>
      )}
      {q && !shown.length && <p className="nt-muted">Nothing matches “{query}”.</p>}
      {shown.map((g) => {
        const topic = byId.get(g.topic);
        return (
          <section key={g.topic} className="nt-topic">
            <h2>{topic ? <a href={topic.url}>{topic.title}</a> : g.topic}</h2>
            {!topic && <p className="nt-muted">This topic is no longer on the site.</p>}
            {g.note && (
              <div className="nt-item">
                <p className="nt-label">Your note · {date(g.note.at)}</p>
                <p className="nt-text">{g.note.text}</p>
                <div className="nt-actions">
                  {topic && <a href={`${topic.url}#your-notes`}>Edit on the topic</a>}
                  <button
                    type="button"
                    className="nt-link"
                    onClick={() => removeTopicNote(g.topic)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            )}
            {g.highlights
              .sort((a, b) => b[1].at - a[1].at)
              .map(([id, h]) => (
                <div className="nt-item" key={id}>
                  <blockquote>{h.quote}</blockquote>
                  {editing === id ? (
                    <div className="nt-edit">
                      <textarea
                        rows={3}
                        maxLength={NOTE_MAX_CHARS}
                        aria-label="Your note"
                        value={draft}
                        autoFocus
                        onChange={(e) => setDraft(e.target.value)}
                      />
                      <div className="nt-actions">
                        <button type="button" className="btn" onClick={() => saveNote(id, h)}>
                          Save
                        </button>
                        <button type="button" className="btn" onClick={() => setEditing(null)}>
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      {h.note && <p className="nt-text">{h.note}</p>}
                      <p className="nt-label">{date(h.at)}</p>
                      <div className="nt-actions">
                        {topic && <a href={`${topic.url}#hl-${id}`}>Show on the page</a>}
                        <button
                          type="button"
                          className="nt-link"
                          onClick={() => {
                            setEditing(id);
                            setDraft(h.note ?? '');
                          }}
                        >
                          {h.note ? 'Edit note' : 'Add note'}
                        </button>
                        <button
                          type="button"
                          className="nt-link"
                          onClick={() => removeHighlight(id, h)}
                        >
                          Remove
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ))}
          </section>
        );
      })}
    </div>
  );
}
