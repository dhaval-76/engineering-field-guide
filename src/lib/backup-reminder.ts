import type { QuizHistory } from './review';
import { keys, read, write, type NotesMap, type ProgressMap } from './storage';

const MONTH = 30 * 24 * 60 * 60 * 1000;

/**
 * True when there is progress that has gone 30 days without a backup. Clearing browser data wipes
 * it, and only this browser holds it. Measured from the last export, or from the oldest record if
 * there has never been one, so a new reader isn't prompted on day one.
 */
export function backupDue(now = Date.now()): boolean {
  if (read<number>(keys.backupSnooze, 0) > now) return false;
  const stamps = [
    ...Object.values(read<ProgressMap>(keys.progress, {})),
    ...Object.values(read<QuizHistory>(keys.quiz, {})).map((r) => r.last),
    ...Object.values(read<NotesMap>(keys.notes, {})).map((n) => n.at),
  ];
  if (!stamps.length) return false;
  const last = read<number | null>(keys.lastExport, null);
  if (last !== null && !stamps.some((t) => t > last)) return false; // nothing new since the backup
  return now - (last ?? Math.min(...stamps)) >= MONTH;
}

export const snoozeBackup = (now = Date.now()) => write(keys.backupSnooze, now + MONTH);
