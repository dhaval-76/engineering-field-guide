import { useState } from 'react';
import './quiz.css';

interface Item {
  q: string;
  options: string[];
  correct: number;
  whyHtml: string;
}

export default function Quiz({ items }: { items: Item[] }) {
  const [answers, setAnswers] = useState<(number | null)[]>(() => items.map(() => null));
  const score = answers.filter((a, i) => a === items[i]!.correct).length;
  const done = answers.every((a) => a !== null);

  return (
    <div className="quiz">
      {items.map((item, i) => {
        const picked = answers[i];
        return (
          <fieldset key={i} className="quiz-q">
            <legend>
              <span className="quiz-n">{i + 1}.</span> {item.q}
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
                    onClick={() => setAnswers((a) => a.map((x, k) => (k === i ? o : x)))}
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
        <button type="button" className="btn" onClick={() => setAnswers(items.map(() => null))}>
          Reset
        </button>
      </div>
    </div>
  );
}
