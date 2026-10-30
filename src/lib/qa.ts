import type { CollectionEntry } from 'astro:content';

export interface TocHeading {
  depth: number;
  slug: string;
  text: string;
}

/** TOC entries for the interview sections rendered after a topic's prose. */
export function interviewHeadings(data: CollectionEntry<'qa'>['data'] | undefined): TocHeading[] {
  if (!data) return [];
  const out: TocHeading[] = [];
  if (data.questions.length)
    out.push({ depth: 2, slug: 'interview-questions', text: 'Interview questions' });
  if (data.followUps.length)
    out.push({ depth: 2, slug: 'follow-ups', text: 'Follow-ups to expect' });
  if (data.practice.length) out.push({ depth: 2, slug: 'practice', text: 'Practice' });
  if (data.quiz.length) out.push({ depth: 2, slug: 'self-check', text: 'Self-check' });
  return out;
}
