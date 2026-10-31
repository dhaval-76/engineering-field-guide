import type { CollectionEntry } from 'astro:content';
import { md } from './markdown';
import { quizId } from './review';

/** A topic's quiz rendered for the client: Markdown explanations become HTML, each question gets its review id. */
export function quizItems(topicId: string, qa: CollectionEntry<'qa'>['data']) {
  return Promise.all(
    qa.quiz.map(async (x) => ({
      id: quizId(topicId, x.q),
      q: x.q,
      options: x.options,
      correct: x.correct,
      whyHtml: await md(x.why),
    })),
  );
}
export type QuizItem = Awaited<ReturnType<typeof quizItems>>[number];
