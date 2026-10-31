import { useEffect, useRef, useState, type KeyboardEvent, type TouchEvent } from 'react';
import './walkthrough.css';

type Tone = 'accent' | 'ok' | 'warn' | 'bad' | 'muted' | 'info' | 'plum';
type Cell =
  string | number | { v: string | number; tone?: Tone | undefined; tag?: string | undefined };

interface Panel {
  kind: 'array' | 'list' | 'rows' | 'flow' | 'kv' | 'stack' | 'queue' | 'note';
  label?: string | undefined;
  items: Cell[];
  pairs: [Cell, Cell][];
  text?: string | undefined;
}

interface Frame {
  captionHtml: string;
  panels: Panel[];
}

interface Props {
  title: string;
  frames: Frame[];
  /** per-walkthrough wording for a colour, where the default meaning doesn't fit */
  legend?: Partial<Record<Tone, string>> | undefined;
}

const TONES: Tone[] = ['accent', 'info', 'ok', 'warn', 'bad', 'plum', 'muted'];
const DEFAULT_LEGEND: Record<Tone, string> = {
  accent: 'acted on in this step',
  info: 'active / in progress',
  ok: 'done / succeeded',
  warn: 'changed / at risk',
  bad: 'failed / removed',
  plum: 'another actor',
  muted: 'gone / ruled out',
};

function tonesUsed(frames: Frame[]): Set<Tone> {
  const used = new Set<Tone>();
  const add = (c: Cell) => {
    if (typeof c === 'object' && c.tone) used.add(c.tone);
  };
  for (const f of frames)
    for (const p of f.panels) {
      p.items.forEach(add);
      p.pairs.forEach(([k, v]) => (add(k), add(v)));
    }
  return used;
}

const norm = (c: Cell) => (typeof c === 'object' ? c : { v: c, tone: undefined, tag: undefined });

function CellBox({ cell, index }: { cell: Cell; index?: number | undefined }) {
  const c = norm(cell);
  return (
    <div className="wt-cell-wrap">
      <div className={`wt-cell ${c.tone ? `tone-${c.tone}` : ''}`}>{String(c.v)}</div>
      {index !== undefined && <div className="wt-index">{index}</div>}
      {c.tag && <div className="wt-tag">{c.tag}</div>}
    </div>
  );
}

function PanelView({ panel }: { panel: Panel }) {
  const { kind } = panel;
  let body: React.ReactNode;
  if (kind === 'note') body = <p className="wt-note">{panel.text}</p>;
  else if (kind === 'kv')
    body = (
      <table className="wt-kv">
        <tbody>
          {panel.pairs.map(([k, v], i) => (
            <tr key={i}>
              <td>
                <CellBox cell={k} />
              </td>
              <td aria-hidden="true" className="wt-arrow">
                →
              </td>
              <td>
                <CellBox cell={v} />
              </td>
            </tr>
          ))}
          {panel.pairs.length === 0 && (
            <tr>
              <td className="wt-empty">empty</td>
            </tr>
          )}
        </tbody>
      </table>
    );
  else
    body = (
      <div className={`wt-row wt-${kind}`}>
        {panel.items.length === 0 && <span className="wt-empty">empty</span>}
        {panel.items.map((cell, i) => (
          <div className="wt-item" key={i}>
            <CellBox cell={cell} index={kind === 'array' ? i : undefined} />
            {kind === 'list' && i < panel.items.length - 1 && (
              <span className="wt-link" aria-hidden="true">
                ⇄
              </span>
            )}
            {kind === 'flow' && i < panel.items.length - 1 && (
              <span className="wt-down" aria-hidden="true">
                ↓
              </span>
            )}
          </div>
        ))}
      </div>
    );
  return (
    <div className="wt-panel">
      {panel.label && <div className="wt-label">{panel.label}</div>}
      {body}
    </div>
  );
}

export default function WalkthroughPlayer({ title, frames, legend }: Props) {
  const [i, setI] = useState(0);
  const [playing, setPlaying] = useState(false);
  const touchX = useRef<number | null>(null);
  const last = frames.length - 1;
  const used = tonesUsed(frames);

  useEffect(() => {
    if (!playing) return;
    if (i >= last) {
      setPlaying(false);
      return;
    }
    const t = setTimeout(() => setI((n) => Math.min(last, n + 1)), 2200);
    return () => clearTimeout(t);
  }, [playing, i, last]);

  const go = (n: number) => {
    setPlaying(false);
    setI(Math.max(0, Math.min(last, n)));
  };

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'ArrowRight') go(i + 1);
    else if (e.key === 'ArrowLeft') go(i - 1);
    else return;
    e.preventDefault();
  };
  const onTouchStart = (e: TouchEvent) => (touchX.current = e.touches[0]?.clientX ?? null);
  const onTouchEnd = (e: TouchEvent) => {
    const start = touchX.current;
    const end = e.changedTouches[0]?.clientX;
    touchX.current = null;
    if (start == null || end == null || Math.abs(end - start) < 40) return;
    go(end < start ? i + 1 : i - 1);
  };

  return (
    <figure className="wt" aria-roledescription="step-by-step walkthrough" aria-label={title}>
      <div className="wt-head">
        <span className="wt-title">{title}</span>
        <span className="wt-count" aria-live="polite">
          Step {i + 1} of {frames.length}
        </span>
      </div>
      <div
        className="wt-stage"
        tabIndex={0}
        onKeyDown={onKey}
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
      >
        {/* Every step is rendered in the same grid cell and only the current one is visible,
            so the box is as tall as the tallest step and the buttons never move. */}
        {frames.map((f, n) => (
          <div className={`wt-frame${n === i ? ' on' : ''}`} key={n} aria-hidden={n !== i}>
            {f.panels.map((p, k) => (
              <PanelView panel={p} key={k} />
            ))}
          </div>
        ))}
      </div>
      {used.size > 0 && (
        <ul className="wt-legend" aria-label="Colour key">
          {TONES.filter((t) => used.has(t)).map((t) => (
            <li key={t}>
              <span className={`wt-swatch tone-${t}`} aria-hidden="true" />
              {legend?.[t] ?? DEFAULT_LEGEND[t]}
            </li>
          ))}
        </ul>
      )}
      <figcaption className="wt-caption">
        {frames.map((f, n) => (
          <div
            className={`wt-frame-caption${n === i ? ' on' : ''}`}
            key={n}
            aria-hidden={n !== i}
            dangerouslySetInnerHTML={{ __html: f.captionHtml }}
          />
        ))}
      </figcaption>
      <div className="wt-dots" role="group" aria-label="Jump to step">
        {frames.map((_, n) => (
          <button
            key={n}
            type="button"
            className={n === i ? 'on' : n < i ? 'seen' : ''}
            aria-label={`Step ${n + 1}`}
            aria-current={n === i ? 'step' : undefined}
            onClick={() => go(n)}
          />
        ))}
      </div>
      <div className="wt-controls">
        <button
          type="button"
          onClick={() => go(i - 1)}
          disabled={i === 0}
          aria-label="Previous step"
        >
          ← Prev
        </button>
        <button
          type="button"
          className="wt-play"
          onClick={() => (i >= last ? go(0) : setPlaying((p) => !p))}
          aria-label={playing ? 'Pause' : i >= last ? 'Restart' : 'Play'}
        >
          {playing ? '❚❚ Pause' : i >= last ? '↺ Restart' : '▶ Play'}
        </button>
        <button
          type="button"
          onClick={() => go(i + 1)}
          disabled={i === last}
          aria-label="Next step"
        >
          Next →
        </button>
      </div>
    </figure>
  );
}
