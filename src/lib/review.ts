/**
 * Spaced repetition for quiz questions (Leitner boxes). Each answer moves a question between boxes:
 * right moves it up one box, wrong sends it back to box 0. A question is due again once the box's
 * interval has passed since it was last answered. Stored in this browser under `efg:quiz`.
 */
import { broadcast, keys, read, write } from './storage';

export interface QuizRecord {
  /** 0 = got it wrong last time; 5 = remembered over about a month. */
  box: number;
  /** When it should be asked again (epoch ms). */
  due: number;
  /** Last answered (epoch ms). */
  last: number;
  right: number;
  wrong: number;
}
export type QuizHistory = Record<string, QuizRecord>;

const DAY = 24 * 60 * 60 * 1000;
/** Days until a question in each box is due again. Box 0 is due straight away. */
export const BOX_DAYS = [0, 1, 3, 7, 16, 35] as const;
export const MASTERED_BOX = 4;

/** Stable id for a quiz question: its topic plus a hash of the question text. */
export function quizId(topicId: string, question: string): string {
  let h = 0x811c9dc5;
  for (let i = 0; i < question.length; i++) h = Math.imul(h ^ question.charCodeAt(i), 0x01000193);
  return `${topicId}#${(h >>> 0).toString(36)}`;
}

export const loadHistory = (): QuizHistory => read<QuizHistory>(keys.quiz, {});

export function recordAnswer(id: string, correct: boolean, now = Date.now()): QuizRecord {
  const history = loadHistory();
  const prev = history[id];
  const box = correct ? Math.min((prev?.box ?? 0) + 1, BOX_DAYS.length - 1) : 0;
  const rec: QuizRecord = {
    box,
    due: now + BOX_DAYS[box]! * DAY,
    last: now,
    right: (prev?.right ?? 0) + (correct ? 1 : 0),
    wrong: (prev?.wrong ?? 0) + (correct ? 0 : 1),
  };
  history[id] = rec;
  write(keys.quiz, history);
  broadcast(keys.quiz);
  return rec;
}

export type ReviewState = 'new' | 'due' | 'learning' | 'mastered';
export function reviewState(rec: QuizRecord | undefined, now = Date.now()): ReviewState {
  if (!rec) return 'new';
  if (rec.due <= now) return 'due';
  return rec.box >= MASTERED_BOX ? 'mastered' : 'learning';
}
