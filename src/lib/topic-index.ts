import { getNav } from './nav';
import { topicUrl, trackUrl } from './url';

/** Lightweight topic/track lists for client islands. */
export async function getTopicIndex() {
  const nav = await getNav();
  return {
    tracks: nav.map((n) => ({
      id: n.track.id,
      title: n.track.data.title,
      url: trackUrl(n.track.id),
      total: n.topics.length,
    })),
    topics: nav.flatMap((n) =>
      n.topics.map((t) => ({
        id: t.id,
        title: t.data.title,
        url: topicUrl(t.id),
        track: n.track.id,
      })),
    ),
  };
}
