import { useEffect, useMemo, useState } from 'react';
import { keys, read } from '~/lib/storage';
import {
  loadHistory,
  recordAnswer,
  reviewState,
  type QuizHistory,
  type ReviewState,
} from '~/lib/review';
import './quiz.css';
import './review.css';

interface Question {
  id: string;
  q: string;
  options: string[];
  correct: number;
  whyHtml: string;
  topic: string;
  topicTitle: string;
  topicUrl: string;
  track: string;
}
interface Props {
  dataUrl: string;
  tracks: { id: string; title: string }[];
}

const ROUND = 10;

function shuffle<T>(xs: T[]): T[] {
  const a = [...xs];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j]!, a[i]!];
  }
  return a;
}

/** Quiz questions you got wrong or haven't seen in a while, one at a time (see lib/review.ts). */
export default function Review({ dataUrl, tracks }: Props) {
  const [questions, setQuestions] = useState<Question[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [history, setHistory] = useState<QuizHistory>({});
  const [completed, setCompleted] = useState<Set<string>>(new Set());
  const [track, setTrack] = useState('');
  const [onlyRead, setOnlyRead] = useState(true);
  const [round, setRound] = useState<Question[] | null>(null);
  const [at, setAt] = useState(0);
  const [picked, setPicked] = useState<number | null>(null);
  const [score, setScore] = useState(0);

  useEffect(() => {
    fetch(dataUrl)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then(setQuestions)
      .catch(() => setFailed(true));
    const load = () => {
      setHistory(loadHistory());
      const done = Object.keys(read<Record<string, number>>(keys.progress, {}));
      setCompleted(new Set(done));
      if (!done.length) setOnlyRead(false);
    };
    load();
    addEventListener('storage', load);
    return () => removeEventListener('storage', load);
  }, [dataUrl]);

  const inTrack = useMemo(
    () => (questions ?? []).filter((q) => !track || q.track === track),
    [questions, track],
  );
  const groups = useMemo(() => {
    const g: Record<ReviewState, Question[]> = { due: [], new: [], learning: [], mastered: [] };
    const now = Date.now();
    for (const q of inTrack) g[reviewState(history[q.id], now)].push(q);
    // Missed questions first, then the ones that have waited longest.
    g.due.sort(
      (a, b) => history[a.id]!.box - history[b.id]!.box || history[a.id]!.due - history[b.id]!.due,
    );
    return g;
  }, [inTrack, history]);
  const fresh = onlyRead ? groups.new.filter((q) => completed.has(q.topic)) : groups.new;

  // Number keys pick an option; Enter or n goes to the next question.
  useEffect(() => {
    if (!round) return;
    const onKey = (e: KeyboardEvent) => {
      if (
        e.metaKey ||
        e.ctrlKey ||
        e.altKey ||
        (e.target as HTMLElement).closest('input, select, textarea')
      )
        return;
      const q = round[at];
      if (!q) return;
      const n = Number(e.key);
      if (picked === null && n >= 1 && n <= q.options.length) choose(n - 1);
      else if (picked !== null && (e.key === 'Enter' || e.key === 'n')) next();
    };
    addEventListener('keydown', onKey);
    return () => removeEventListener('keydown', onKey);
  });

  if (failed)
    return (
      <p className="rv-muted">
        The question list could not be loaded. Reload the page to try again.
      </p>
    );
  if (!questions) return <p className="rv-muted">Loading questions…</p>;

  function start(list: Question[]) {
    setRound(list.slice(0, ROUND));
    setAt(0);
    setPicked(null);
    setScore(0);
  }
  function choose(o: number) {
    const q = round![at]!;
    setPicked(o);
    if (o === q.correct) setScore((s) => s + 1);
    recordAnswer(q.id, o === q.correct);
  }
  function next() {
    setPicked(null);
    setAt((i) => i + 1);
  }
  function finish() {
    setRound(null);
    setHistory(loadHistory());
  }

  if (round) {
    const q = round[at];
    if (!q) {
      const missed = round.length - score;
      return (
        <section className="rv-card rv-done" aria-live="polite">
          <h2>
            {score} of {round.length} right
          </h2>
          <p>
            {missed
              ? `${missed} missed question${missed === 1 ? ' comes' : 's come'} back next time you review. The rest return later, and later again each time you get them right.`
              : 'Each of these comes back later, and later again every time you get it right.'}
          </p>
          <div className="rv-actions">
            <button type="button" className="btn primary" onClick={finish}>
              Back to review
            </button>
          </div>
        </section>
      );
    }
    const before = history[q.id];
    return (
      <section className="rv-card" aria-labelledby="rv-q">
        <p className="rv-meta">
          <span>
            Question {at + 1} of {round.length}
          </span>
          <a href={q.topicUrl}>{q.topicTitle}</a>
        </p>
        <div className="rv-bar" aria-hidden="true">
          <span style={{ width: `${(at / round.length) * 100}%` }} />
        </div>
        <fieldset className="quiz-q">
          <legend id="rv-q">
            {q.q}
            {picked === null && before && (
              <span className={`quiz-seen ${before.box === 0 ? 'missed' : ''}`}>
                {before.box === 0 ? 'Missed last time' : `Right ${before.right}×`}
              </span>
            )}
          </legend>
          <div className="quiz-options">
            {q.options.map((opt, o) => {
              const state =
                picked === null ? '' : o === q.correct ? 'right' : o === picked ? 'wrong' : 'dim';
              return (
                <button
                  key={o}
                  type="button"
                  className={`quiz-opt ${state}`}
                  disabled={picked !== null}
                  onClick={() => choose(o)}
                >
                  <kbd aria-hidden="true">{o + 1}</kbd> {opt}
                </button>
              );
            })}
          </div>
          {picked !== null && (
            <div className="quiz-why" role="status">
              <strong>{picked === q.correct ? 'Correct. ' : 'Not quite. '}</strong>
              <span dangerouslySetInnerHTML={{ __html: q.whyHtml }} />
            </div>
          )}
        </fieldset>
        <div className="rv-actions">
          {picked !== null && (
            <button type="button" className="btn primary" onClick={next} autoFocus>
              {at + 1 < round.length ? 'Next question' : 'See result'}
            </button>
          )}
          <button type="button" className="btn" onClick={finish}>
            Stop
          </button>
        </div>
      </section>
    );
  }

  const stats: [ReviewState, string, string][] = [
    ['due', 'Due now', 'missed, or waiting long enough to ask again'],
    ['learning', 'Learning', 'answered right, coming back later'],
    ['mastered', 'Remembered', 'right several times in a row'],
    ['new', 'Not tried', 'never answered in this browser'],
  ];
  const dueByTopic = new Map<string, { title: string; url: string; n: number }>();
  for (const q of groups.due) {
    const t = dueByTopic.get(q.topic) ?? { title: q.topicTitle, url: q.topicUrl, n: 0 };
    t.n++;
    dueByTopic.set(q.topic, t);
  }

  return (
    <div className="rv">
      <label className="rv-filter">
        <span>Track</span>
        <select value={track} onChange={(e) => setTrack(e.target.value)}>
          <option value="">All tracks</option>
          {tracks.map((t) => (
            <option key={t.id} value={t.id}>
              {t.title}
            </option>
          ))}
        </select>
      </label>

      <dl className="rv-stats">
        {stats.map(([k, label, hint]) => (
          <div key={k} className={`rv-stat rv-${k}`}>
            <dt>{label}</dt>
            <dd>{groups[k].length}</dd>
            <dd className="rv-hint">{hint}</dd>
          </div>
        ))}
      </dl>

      <section className="rv-card">
        <h2>Due for review</h2>
        {groups.due.length ? (
          <>
            <p>
              {groups.due.length} question{groups.due.length === 1 ? '' : 's'} to go over, missed
              ones first.
            </p>
            <div className="rv-actions">
              <button type="button" className="btn primary" onClick={() => start(groups.due)}>
                Review {Math.min(ROUND, groups.due.length)} now
              </button>
            </div>
          </>
        ) : (
          <p className="rv-muted">
            {Object.keys(history).length
              ? 'Nothing is due. Questions come back here as their wait runs out.'
              : 'Answer some questions to start: try new ones below, or use the self-check at the end of any topic.'}
          </p>
        )}
      </section>

      <section className="rv-card">
        <h2>New questions</h2>
        <label className="rv-check">
          <input
            type="checkbox"
            checked={onlyRead}
            disabled={!completed.size}
            onChange={(e) => setOnlyRead(e.target.checked)}
          />
          Only from topics I have marked complete
        </label>
        <p>
          {fresh.length} question{fresh.length === 1 ? '' : 's'} you haven't answered
          {onlyRead ? ' from completed topics' : ''}.
        </p>
        <div className="rv-actions">
          <button
            type="button"
            className="btn"
            disabled={!fresh.length}
            onClick={() => start(shuffle(fresh))}
          >
            Try {Math.min(ROUND, fresh.length)} new
          </button>
        </div>
      </section>

      {dueByTopic.size > 0 && (
        <section>
          <h2>Due by topic</h2>
          <ul className="rv-topics">
            {[...dueByTopic.values()]
              .sort((a, b) => b.n - a.n)
              .map((t) => (
                <li key={t.url}>
                  <a href={t.url}>{t.title}</a>
                  <span className="rv-num">{t.n}</span>
                </li>
              ))}
          </ul>
        </section>
      )}
    </div>
  );
}
