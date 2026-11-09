import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react';
import { backupDue } from '~/lib/backup-reminder';
import { keys, read, storedKeys, write } from '~/lib/storage';
import {
  applyImport,
  buildBundle,
  deleteEverything,
  deleteSection,
  encodeSyncCode,
  ImportError,
  loadAll,
  MAX_IMPORT_BYTES,
  planImport,
  readBackup,
  SECTIONS,
  syncLink,
  type ImportMode,
  type ParsedBackup,
  type SectionId,
  type UserData,
} from '~/lib/user-data';
import './data.css';

interface Props {
  topics: { id: string; title: string; url: string }[];
  pageUrl: string;
}

/** Longest link still reliable to scan off a screen; beyond this, a QR code is too dense. */
const QR_MAX_CHARS = 1800;
const plural = (n: number, one: string, many = `${one}s`) => `${n} ${n === 1 ? one : many}`;
const when = (t: number | null) => (t ? new Date(t).toLocaleString() : 'Never');
const problem = (err: unknown) =>
  err instanceof ImportError
    ? err.message
    : 'Something went wrong reading that. Nothing was changed.';

function countOf(data: UserData, id: SectionId): number {
  const v = data[id];
  return Array.isArray(v) ? v.length : Object.keys(v).length;
}

function summary(data: UserData, id: SectionId): string {
  const n = countOf(data, id);
  switch (id) {
    case 'progress':
      return plural(n, 'topic') + ' completed';
    case 'bookmarks':
      return plural(n, 'saved topic');
    case 'recent':
      return plural(n, 'history entry', 'history entries');
    case 'notes':
      return plural(n, 'topic') + ' with notes';
    case 'highlights':
      return plural(n, 'highlight');
    case 'quiz': {
      const all = Object.values(data.quiz);
      const right = all.reduce((s, r) => s + r.right, 0);
      const wrong = all.reduce((s, r) => s + r.wrong, 0);
      return `${plural(n, 'question')} answered: ${right} right, ${wrong} wrong`;
    }
    case 'preferences': {
      const p = data.preferences;
      return [
        `Theme: ${p.theme ?? 'follows your system'}`,
        `Code language: ${p.lang ?? 'Java'}`,
        `Reading mode: ${p.focus ? 'on' : 'off'}`,
        `Code wrap: ${p.wrap ? 'on' : 'off'}`,
      ].join(' · ');
    }
  }
}

/** A small QR code as one SVG path. The library is loaded only when someone asks for a code. */
async function qrPath(text: string) {
  const { default: qrcode } = await import('qrcode-generator');
  const qr = qrcode(0, 'L');
  qr.addData(text, 'Byte');
  qr.make();
  const size = qr.getModuleCount();
  let d = '';
  for (let y = 0; y < size; y++)
    for (let x = 0; x < size; x++) if (qr.isDark(y, x)) d += `M${x} ${y}h1v1h-1z`;
  return { size, d };
}

/** View, back up, restore, copy between devices and delete everything stored in this browser. */
export default function YourData({ topics, pageUrl }: Props) {
  const [data, setData] = useState<UserData | null>(null);
  const [chosen, setChosen] = useState<Set<SectionId>>(() => new Set(SECTIONS.map((s) => s.id)));
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [code, setCode] = useState('');
  const [qr, setQr] = useState<{ size: number; d: string } | null>(null);
  const [qrNote, setQrNote] = useState('');
  const [pasted, setPasted] = useState('');
  const [incoming, setIncoming] = useState<ParsedBackup | null>(null);
  const [mode, setMode] = useState<ImportMode>('merge');
  const [importing, setImporting] = useState<Set<SectionId>>(new Set());
  const preview = useRef<HTMLElement>(null);

  const reload = () => setData(loadAll());
  const receive = async (text: string) => {
    setError('');
    setMessage('');
    try {
      const parsed = await readBackup(text);
      setIncoming(parsed);
      setImporting(new Set(Object.keys(parsed.bundle.data) as SectionId[]));
      setMode('merge');
      setPasted('');
    } catch (err) {
      setIncoming(null);
      setError(problem(err));
    }
  };

  useEffect(() => {
    reload();
    addEventListener('storage', reload);
    addEventListener('efg:storage', reload);
    // Arrived from a QR code: show the preview, and drop the code from the address bar and history.
    const fromLink = () => {
      if (!location.hash.startsWith('#sync=')) return;
      const link = location.hash;
      history.replaceState(null, '', location.pathname);
      void receive(link);
    };
    fromLink();
    addEventListener('hashchange', fromLink);
    return () => {
      removeEventListener('hashchange', fromLink);
      removeEventListener('storage', reload);
      removeEventListener('efg:storage', reload);
    };
  }, []);

  useEffect(() => {
    if (incoming) preview.current?.scrollIntoView({ block: 'start' });
  }, [incoming]);

  const plan = useMemo(
    () => (data && incoming ? planImport(data, incoming.bundle, mode, importing) : null),
    [data, incoming, mode, importing],
  );
  const titles = useMemo(() => new Map(topics.map((t) => [t.id, t])), [topics]);

  if (!data) return null;
  const empty = SECTIONS.every((s) => countOf(data, s.id) === 0);
  const picked = SECTIONS.filter((s) => chosen.has(s.id)).map((s) => s.id);
  const toggle = (set: Set<SectionId>, id: SectionId) => {
    const next = new Set(set);
    if (!next.delete(id)) next.add(id);
    return next;
  };
  const noteExport = () => {
    if (picked.includes('progress')) write(keys.lastExport, Date.now());
  };
  const resetShare = () => {
    setCode('');
    setQr(null);
    setQrNote('');
  };

  const download = () => {
    const blob = new Blob([JSON.stringify(buildBundle(data, picked), null, 2)], {
      type: 'application/json',
    });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `field-guide-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    noteExport();
    setMessage('Backup downloaded.');
  };
  const makeCode = async () => {
    setError('');
    try {
      const next = await encodeSyncCode(buildBundle(data, picked));
      setCode(next);
      setQr(null);
      setQrNote('');
      return next;
    } catch {
      setError('This browser cannot create sync codes. Download a backup file instead.');
      return null;
    }
  };
  const copyCode = async () => {
    const value = await makeCode();
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      setMessage('Code copied. Paste it on the other device, under Restore.');
    } catch {
      setMessage('Your browser blocked copying. Select the code below and copy it.');
    }
    noteExport();
  };
  const showQr = async () => {
    const value = await makeCode();
    if (!value) return;
    const link = syncLink(pageUrl, value);
    if (link.length > QR_MAX_CHARS) {
      setQrNote(
        'That is too much data for one QR code. Untick some sections and try again, or use Copy sync code, which has no such limit.',
      );
      return;
    }
    setQr(await qrPath(link));
    noteExport();
  };
  const openFile = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    if (file.size > MAX_IMPORT_BYTES) {
      setIncoming(null);
      return setError('This file is too large to be a backup.');
    }
    await receive(await file.text());
  };
  const apply = () => {
    if (!plan) return;
    try {
      applyImport(plan);
    } catch (err) {
      return setError(problem(err));
    }
    const prefs = plan.rows.find((r) => r.id === 'preferences');
    const prefsChanged = !!prefs && prefs.added + prefs.updated + prefs.removed > 0;
    setIncoming(null);
    reload();
    setMessage(prefsChanged ? 'Imported. Reloading to apply your preferences…' : 'Imported.');
    if (prefsChanged) setTimeout(() => location.reload(), 1200);
  };
  const removeSection = (id: SectionId, label: string) => {
    if (!confirm(`Delete ${label.toLowerCase()} from this browser? This cannot be undone.`)) return;
    deleteSection(id);
    reload();
    setMessage(`${label}: deleted.`);
    if (id === 'preferences') setTimeout(() => location.reload(), 800);
  };
  const removeAll = () => {
    if (!confirm('Delete everything this site has stored in this browser? This cannot be undone.'))
      return;
    deleteEverything();
    location.reload();
  };

  const quizByTopic = new Map<string, { right: number; wrong: number; n: number }>();
  for (const [id, r] of Object.entries(data.quiz)) {
    const topic = id.split('#')[0]!;
    const t = quizByTopic.get(topic) ?? { right: 0, wrong: 0, n: 0 };
    quizByTopic.set(topic, { right: t.right + r.right, wrong: t.wrong + r.wrong, n: t.n + 1 });
  }
  const changes = plan?.rows.reduce((s, r) => s + r.added + r.updated + r.removed, 0) ?? 0;

  return (
    <div className="dt">
      {backupDue() && (
        <p className="dt-reminder" role="status">
          It has been over 30 days since your progress was backed up.
        </p>
      )}
      <p className="dt-status" role="status" aria-live="polite">
        {message}
      </p>
      {error && (
        <p className="dt-error" role="alert">
          {error}
        </p>
      )}

      <h2>What is stored</h2>
      <p className="dt-muted">Tick what to include when you back up or copy to another device.</p>
      <ul className="dt-sections">
        {SECTIONS.map(({ id, label }) => {
          const n = countOf(data, id);
          return (
            <li key={id}>
              <label>
                <input
                  type="checkbox"
                  checked={chosen.has(id)}
                  onChange={() => {
                    setChosen(toggle(chosen, id));
                    resetShare();
                  }}
                />
                <strong>{label}</strong>
              </label>
              <p>{summary(data, id)}</p>
              {id === 'quiz' && quizByTopic.size > 0 && (
                <details>
                  <summary>By topic</summary>
                  <ul className="dt-quiz">
                    {[...quizByTopic]
                      .sort((a, b) => b[1].wrong - a[1].wrong)
                      .map(([topic, t]) => (
                        <li key={topic}>
                          {titles.has(topic) ? (
                            <a href={titles.get(topic)!.url}>{titles.get(topic)!.title}</a>
                          ) : (
                            <span>{topic}</span>
                          )}
                          <span className="dt-num">
                            {t.right} right, {t.wrong} wrong
                          </span>
                        </li>
                      ))}
                  </ul>
                </details>
              )}
              <details>
                <summary>Raw data</summary>
                <pre tabIndex={0}>{JSON.stringify(data[id], null, 2)}</pre>
              </details>
              <button
                type="button"
                className="btn danger"
                disabled={n === 0}
                onClick={() => removeSection(id, label)}
              >
                Delete
              </button>
            </li>
          );
        })}
      </ul>
      <p className="dt-muted">
        This device only, never exported: offline copy{' '}
        {read<boolean | null>('offline-optin', null) === false ? 'removed' : 'on'}; last update
        check {when(read<number | null>('offline-checked', null))}; last backup{' '}
        {when(read<number | null>(keys.lastExport, null))}.
      </p>

      <h2>Back up</h2>
      <p className="dt-muted">
        A backup is a small file you keep. Clearing this browser's data erases everything above, so
        keep one somewhere safe.
      </p>
      <div className="dt-actions">
        <button type="button" className="btn primary" disabled={!picked.length} onClick={download}>
          Download backup
        </button>
      </div>

      <h2>Copy to another device</h2>
      <p className="dt-muted">
        No account needed. The data goes directly between your devices and never passes through a
        server. A code holds your notes and history, so treat it like the data itself.
      </p>
      <div className="dt-actions">
        <button type="button" className="btn" disabled={!picked.length} onClick={copyCode}>
          Copy sync code
        </button>
        <button type="button" className="btn" disabled={!picked.length} onClick={showQr}>
          Show QR code
        </button>
      </div>
      {qrNote && <p className="dt-muted">{qrNote}</p>}
      {qr && (
        <figure className="dt-qr">
          <svg
            viewBox={`-4 -4 ${qr.size + 8} ${qr.size + 8}`}
            role="img"
            aria-label="QR code that opens this page on another device with your data ready to import"
          >
            <rect x="-4" y="-4" width={qr.size + 8} height={qr.size + 8} fill="#fff" />
            <path d={qr.d} fill="#000" shapeRendering="crispEdges" />
          </svg>
          <figcaption>
            Point the other device's camera at this. It opens the guide there and shows a preview
            before anything changes.
          </figcaption>
        </figure>
      )}
      {code && !qr && !qrNote && (
        <textarea className="dt-code" readOnly rows={4} value={code} aria-label="Your sync code" />
      )}

      <h2>Restore</h2>
      <p className="dt-muted">
        Open a backup file or paste a sync code. You will see a preview first.
      </p>
      <div className="dt-actions">
        <label className="btn">
          Open backup file
          <input
            className="visually-hidden"
            type="file"
            accept=".json,application/json"
            onChange={openFile}
          />
        </label>
      </div>
      <div className="dt-paste">
        <textarea
          rows={3}
          placeholder="Paste a sync code here"
          aria-label="Paste a sync code"
          value={pasted}
          onChange={(e) => setPasted(e.target.value)}
        />
        <button
          type="button"
          className="btn"
          disabled={!pasted.trim()}
          onClick={() => receive(pasted)}
        >
          Preview
        </button>
      </div>

      {incoming && plan && (
        <section className="dt-preview" ref={preview} aria-labelledby="dt-preview-h" tabIndex={-1}>
          <h3 id="dt-preview-h">Preview</h3>
          <p className="dt-muted">
            From a backup made {new Date(incoming.bundle.exportedAt).toLocaleString()}. Nothing has
            changed yet.
          </p>
          <fieldset>
            <legend>How to import</legend>
            <label>
              <input
                type="radio"
                name="dt-mode"
                checked={mode === 'merge'}
                onChange={() => setMode('merge')}
              />
              <span>
                <strong>Merge.</strong> Keeps both; where the same item is on both, the newest wins.
                Preferences on this device stay as they are.
              </span>
            </label>
            <label>
              <input
                type="radio"
                name="dt-mode"
                checked={mode === 'replace'}
                onChange={() => setMode('replace')}
              />
              <span>
                <strong>Replace.</strong> The ticked sections on this device become exactly what is
                in the backup.
              </span>
            </label>
          </fieldset>
          <div className="dt-table" tabIndex={0}>
            <table>
              <thead>
                <tr>
                  <th scope="col">Section</th>
                  <th scope="col">Now</th>
                  <th scope="col">After</th>
                  <th scope="col">New</th>
                  <th scope="col">Updated</th>
                  <th scope="col">Removed</th>
                </tr>
              </thead>
              <tbody>
                {SECTIONS.filter((s) => incoming.bundle.data[s.id] !== undefined).map((s) => {
                  const row = plan.rows.find((r) => r.id === s.id);
                  return (
                    <tr key={s.id}>
                      <th scope="row">
                        <label>
                          <input
                            type="checkbox"
                            checked={importing.has(s.id)}
                            onChange={() => setImporting(toggle(importing, s.id))}
                          />
                          {s.label}
                        </label>
                      </th>
                      <td>{row?.before ?? '–'}</td>
                      <td>{row?.after ?? '–'}</td>
                      <td>{row?.added ?? '–'}</td>
                      <td>{row?.updated ?? '–'}</td>
                      <td className={row?.removed ? 'dt-bad' : ''}>{row?.removed ?? '–'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {incoming.ignored.length > 0 && (
            <p className="dt-muted">
              Skipped parts this version doesn't know: {incoming.ignored.join(', ')}.
            </p>
          )}
          <div className="dt-actions">
            <button
              type="button"
              className={`btn ${mode === 'replace' ? 'danger-solid' : 'primary'}`}
              disabled={!changes}
              onClick={apply}
            >
              {changes
                ? mode === 'replace'
                  ? 'Replace my data'
                  : 'Merge into my data'
                : 'Nothing to change'}
            </button>
            <button type="button" className="btn" onClick={() => setIncoming(null)}>
              Cancel
            </button>
          </div>
        </section>
      )}

      <h2>Delete everything</h2>
      <p className="dt-muted">
        Removes all {plural(storedKeys().length, 'stored item')} from this browser. The offline copy
        of the site stays; manage that on the Offline page.
      </p>
      <div className="dt-actions">
        <button
          type="button"
          className="btn danger"
          disabled={empty && !storedKeys().length}
          onClick={removeAll}
        >
          Delete everything
        </button>
      </div>
    </div>
  );
}
