import { useEffect, useState } from 'react';
import './history.css';
import { read } from '~/lib/storage';
import { LAST_UPDATE, type UpdateChanges } from '~/lib/update-changes';

interface Props {
  /** built file (e.g. `dsa/sliding-window/index.html`) → topic title */
  pageTitles: Record<string, string>;
  base: string;
}

/** What the last offline-copy update brought, from efg:last-update (saved by scripts/offline.ts). */
export default function LastUpdate({ pageTitles, base }: Props) {
  const [changes, setChanges] = useState<UpdateChanges | null | undefined>(undefined);
  useEffect(() => {
    const load = () => setChanges(read<UpdateChanges | null>(LAST_UPDATE, null));
    load();
    addEventListener('storage', load);
    return () => removeEventListener('storage', load);
  }, []);
  if (changes === undefined) return null;

  if (!changes)
    return (
      <section className="lu">
        <h2>In your last update</h2>
        <p className="lu-muted">
          When the offline copy downloads a new version, the topics it changed are listed here.
        </p>
      </section>
    );

  const topics = (paths: string[]) =>
    paths.filter((p) => p in pageTitles).map((p) => ({ path: p, title: pageTitles[p]! }));
  const added = topics(changes.added);
  const changed = topics(changes.changed);
  const removed = changes.removed.filter(
    (p) => p.endsWith('index.html') && p.split('/').length === 3,
  );
  const topicFiles = new Set([...added, ...changed].map((t) => t.path));
  const other = [...changes.added, ...changes.changed].filter((p) => !topicFiles.has(p)).length;
  const when = new Date(changes.at).toLocaleString(undefined, {
    dateStyle: 'long',
    timeStyle: 'short',
  });
  const link = (t: { path: string; title: string }) => (
    <li key={t.path}>
      <a href={base + t.path.replace(/index\.html$/, '')}>{t.title}</a>
    </li>
  );

  return (
    <section className="lu">
      <h2>In your last update</h2>
      <p className="lu-muted">
        Downloaded {when} (version {changes.version}).
      </p>
      {added.length > 0 && (
        <>
          <h3>New topics</h3>
          <ul>{added.map(link)}</ul>
        </>
      )}
      {changed.length > 0 && (
        <>
          <h3>Changed topics</h3>
          <ul>{changed.map(link)}</ul>
        </>
      )}
      {removed.length > 0 && (
        <p className="lu-muted">
          {removed.length} page{removed.length === 1 ? ' was' : 's were'} removed or moved.
        </p>
      )}
      <p className="lu-muted">
        {added.length || changed.length ? 'Also ' : ''}
        {other} other file{other === 1 ? '' : 's'} changed (other pages, styles, scripts and the
        search index).
      </p>
    </section>
  );
}
