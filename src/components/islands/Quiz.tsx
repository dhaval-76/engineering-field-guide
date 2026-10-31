import { useEffect, useState } from 'react';
import { loadHistory, recordAnswer, type QuizHistory } from '~/lib/review';
import './quiz.css';

interface Item {
  /** review id (lib/review.ts quizId) */
  id: string;
  q: string;
  options: string[];
  correct: number;
  whyHtml: string;
}

export default function Quiz({ items, reviewUrl }: { items: Item[]; reviewUrl: string }) {
  const [answers, setAnswers] = useState<(number | null)[]>(() => items.map(() => null));
  // What this browser remembers from earlier attempts; loaded after hydration so the HTML matches.
  const [history, setHistory] = useState<QuizHistory>({});
  useEffect(() => setHistory(loadHistory()), []);
  const answer = (i: number, o: number) => {
    setAnswers((a) => a.map((x, k) => (k === i ? o : x)));
    recordAnswer(items[i]!.id, o === items[i]!.correct);
  };
  const score = answers.filter((a, i) => a === items[i]!.correct).length;
  const done = answers.every((a) => a !== null);

  return (
    <div className="quiz">
      {items.map((item, i) => {
        const picked = answers[i];
        const before = history[item.id];
        return (
          <fieldset key={i} className="quiz-q">
            <legend>
              <span className="quiz-n">{i + 1}.</span> {item.q}
              {picked === null && before && (
                <span className={`quiz-seen ${before.box === 0 ? 'missed' : ''}`}>
                  {before.box === 0 ? 'Missed last time' : 'Answered before'}
                </span>
              )}
            </legend>
            <div className="quiz-options">
              {item.options.map((opt, o) => {
                const state =
                  picked === null
                    ? ''
                    : o === item.correct
                      ? 'right'
                      : o === picked
                        ? 'wrong'
                        : 'dim';
                return (
                  <button
                    key={o}
                    type="button"
                    className={`quiz-opt ${state}`}
                    disabled={picked !== null}
                    onClick={() => answer(i, o)}
                  >
                    {opt}
                  </button>
                );
              })}
            </div>
            {picked !== null && (
              <div className="quiz-why" role="status">
                <strong>{picked === item.correct ? 'Correct. ' : 'Not quite. '}</strong>
                <span dangerouslySetInnerHTML={{ __html: item.whyHtml }} />
              </div>
            )}
          </fieldset>
        );
      })}
      <div className="quiz-foot">
        {done ? (
          <span>
            Score: <strong>{score}</strong> / {items.length}
          </span>
        ) : (
          <span>
            {answers.filter((a) => a !== null).length} of {items.length} answered
          </span>
        )}
        <span className="quiz-actions">
          <a href={reviewUrl}>Review later</a>
          <button
            type="button"
            className="btn"
            onClick={() => {
              setHistory(loadHistory());
              setAnswers(items.map(() => null));
            }}
          >
            Reset
          </button>
        </span>
      </div>
    </div>
  );
}
