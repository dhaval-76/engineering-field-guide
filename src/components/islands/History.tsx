import { useEffect, useMemo, useState } from 'react';
import { broadcast, keys, read, write, type RecentList } from '~/lib/storage';
import './history.css';

interface Props {
  topics: { id: string; title: string; url: string; track: string }[];
  tracks: { id: string; title: string }[];
}

const DAY = 24 * 60 * 60 * 1000;
const startOfDay = (t: number) => new Date(t).setHours(0, 0, 0, 0);

/** Today, Yesterday, This week (the 7 days before that), then one group per month. */
function groupLabel(at: number, today: number): string {
  const day = startOfDay(at);
  if (day >= today) return 'Today';
  if (day >= today - DAY) return 'Yesterday';
  if (day >= today - 7 * DAY) return 'This week';
  return new Date(at).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
}

/** Every topic opened in this browser, newest first (efg:recent, written by site.ts). */
export default function History({ topics, tracks }: Props) {
  const [list, setList] = useState<RecentList | null>(null);
  const [done, setDone] = useState<Record<string, number>>({});
  const [query, setQuery] = useState('');

  useEffect(() => {
    const load = () => {
      setList(read<RecentList>(keys.recent, []));
      setDone(read<Record<string, number>>(keys.progress, {}));
    };
    load();
    addEventListener('storage', load);
    return () => removeEventListener('storage', load);
  }, []);

  const byId = useMemo(() => new Map(topics.map((t) => [t.id, t])), [topics]);
  const trackTitle = useMemo(() => new Map(tracks.map((t) => [t.id, t.title])), [tracks]);

  if (!list) return null;
  const save = (next: RecentList) => {
    write(keys.recent, next);
    broadcast(keys.recent);
    setList(next);
  };

  // Topics that were renamed or removed since they were read keep their saved title but no link.
  const q = query.trim().toLowerCase();
  const rows = list
    .map((r) => ({ ...r, topic: byId.get(r.id) }))
    .filter(
      (r) => !q || `${r.topic?.title ?? r.title} ${r.topic?.track ?? ''}`.toLowerCase().includes(q),
    );
  const today = startOfDay(Date.now());
  const groups: { label: string; rows: typeof rows }[] = [];
  for (const r of rows) {
    const label = groupLabel(r.at, today);
    const last = groups.at(-1);
    if (last?.label === label) last.rows.push(r);
    else groups.push({ label, rows: [r] });
  }

  if (!list.length)
    return (
      <p className="hs-muted">
        Nothing here yet. Topics you open are listed here, newest first, and stay in this browser
        only.
      </p>
    );

  return (
    <div className="hs">
      <div className="hs-bar">
        <input
          type="search"
          placeholder="Filter your history"
          aria-label="Filter your history"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button
          type="button"
          className="btn"
          onClick={() =>
            confirm('Clear your whole reading history? Progress and saved topics are kept.') &&
            save([])
          }
        >
          Clear history
        </button>
      </div>
      <p className="hs-muted">
        {list.length} {list.length === 1 ? 'entry' : 'entries'}
        {q && `, ${rows.length} matching`}. A topic appears once for each day you opened it.
      </p>
      {groups.map((g) => (
        <section key={g.label}>
          <h2>{g.label}</h2>
          <ul className="hs-list">
            {g.rows.map((r) => {
              const time = new Date(r.at);
              const when =
                g.label === 'Today' || g.label === 'Yesterday'
                  ? time.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })
                  : time.toLocaleDateString(undefined, {
                      weekday: 'short',
                      day: 'numeric',
                      month: 'short',
                    });
              return (
                <li key={`${r.id}@${r.at}`}>
                  <span className="hs-when">{when}</span>
                  <span className="hs-title">
                    {r.topic ? <a href={r.topic.url}>{r.topic.title}</a> : <span>{r.title}</span>}
                    {done[r.id] ? <span className="hs-done"> · completed</span> : null}
                    <span className="hs-track">
                      {r.topic ? trackTitle.get(r.topic.track) : 'No longer on the site'}
                    </span>
                  </span>
                  <button
                    type="button"
                    className="hs-remove"
                    aria-label={`Remove ${r.topic?.title ?? r.title} from history`}
                    title="Remove from history"
                    onClick={() => save(list.filter((x) => !(x.id === r.id && x.at === r.at)))}
                  >
                    ×
                  </button>
                </li>
              );
            })}
          </ul>
        </section>
      ))}
      {q && !rows.length && <p className="hs-muted">Nothing in your history matches “{query}”.</p>}
    </div>
  );
}
