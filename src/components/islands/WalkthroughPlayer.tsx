import { useEffect, useRef, useState, type KeyboardEvent, type TouchEvent } from 'react';
import './walkthrough.css';

type Tone = 'accent' | 'ok' | 'warn' | 'bad' | 'muted' | 'info';
type Cell =
  string | number | { v: string | number; tone?: Tone | undefined; tag?: string | undefined };

interface Panel {
  kind: 'array' | 'list' | 'kv' | 'stack' | 'queue' | 'note';
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

export default function WalkthroughPlayer({ title, frames }: Props) {
  const [i, setI] = useState(0);
  const [playing, setPlaying] = useState(false);
  const touchX = useRef<number | null>(null);
  const last = frames.length - 1;
  const frame = frames[i]!;

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
        {frame.panels.map((p, k) => (
          <PanelView panel={p} key={k} />
        ))}
      </div>
      <figcaption className="wt-caption" dangerouslySetInnerHTML={{ __html: frame.captionHtml }} />
      <div className="wt-controls">
        <button
          type="button"
          onClick={() => go(i - 1)}
          disabled={i === 0}
          aria-label="Previous step"
        >
          ← Prev
        </button>
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
        <button
          type="button"
          onClick={() => (i >= last ? go(0) : setPlaying((p) => !p))}
          aria-label={playing ? 'Pause' : i >= last ? 'Restart' : 'Play'}
        >
          {playing ? 'Pause' : i >= last ? 'Restart' : 'Play'}
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
