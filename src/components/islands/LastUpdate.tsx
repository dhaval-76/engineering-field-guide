import { useEffect, useState } from 'react';
import './history.css';
import { read } from '~/lib/storage';
import { LAST_UPDATE, type UpdateChanges } from '~/lib/update-changes';

interface Props {
  /** topic id → title */
  topicTitles: Record<string, string>;
  base: string;
}

/** What the last offline-copy update brought, from efg:last-update (saved by scripts/offline.ts). */
export default function LastUpdate({ topicTitles, base }: Props) {
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

  const listed = (ids: string[]) =>
    ids.filter((id) => id in topicTitles).map((id) => ({ id, title: topicTitles[id]! }));
  const added = listed(changes.topics?.added ?? []);
  const changed = listed(changes.topics?.changed ?? []);
  const removed = changes.topics?.removed.length ?? 0;
  const when = new Date(changes.at).toLocaleString(undefined, {
    dateStyle: 'long',
    timeStyle: 'short',
  });
  const link = (t: { id: string; title: string }) => (
    <li key={t.id}>
      <a href={`${base}${t.id}/`}>{t.title}</a>
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
      {removed > 0 && (
        <p className="lu-muted">
          {removed} topic{removed === 1 ? ' was' : 's were'} removed or moved.
        </p>
      )}
      {!changes.topics && (
        <p className="lu-muted">
          This update can't say which topics changed. Changed topics are listed from your next
          update onwards.
        </p>
      )}
      {changes.topics && !added.length && !changed.length && !removed && (
        <p className="lu-muted">No topic content changed. This update was to the site itself.</p>
      )}
      {changes.files !== undefined && (
        <p className="lu-muted">
          {changes.files} file{changes.files === 1 ? '' : 's'} updated in total, including layout,
          styles, scripts and the search index.
        </p>
      )}
    </section>
  );
}
