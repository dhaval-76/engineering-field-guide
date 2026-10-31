import { getEntry } from 'astro:content';
import { getNav } from '~/lib/nav';
import { quizItems } from '~/lib/quiz';
import { topicUrl } from '~/lib/url';

/** Every quiz question on the site, for the /review/ page. Static: built once, saved by the offline copy. */
export async function GET() {
  const questions = [];
  for (const { track, topics } of await getNav()) {
    for (const topic of topics) {
      const qa = await getEntry('qa', topic.id);
      if (!qa?.data.quiz.length) continue;
      for (const item of await quizItems(topic.id, qa.data))
        questions.push({
          ...item,
          topic: topic.id,
          topicTitle: topic.data.title,
          topicUrl: `${topicUrl(topic.id)}#self-check`,
          track: track.id,
        });
    }
  }
  return new Response(JSON.stringify(questions), {
    headers: { 'Content-Type': 'application/json' },
  });
}
