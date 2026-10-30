import { getCollection, getEntry, type CollectionEntry } from 'astro:content';

export type Track = CollectionEntry<'tracks'>;
export type Topic = CollectionEntry<'topics'>;

export interface TrackNav {
  track: Track;
  topics: Topic[];
}

const visible = (t: Topic) => import.meta.env.DEV || !t.data.draft;

let cache: Promise<TrackNav[]> | undefined;

/** All tracks in order, each with its topics in reading order. */
export function getNav(): Promise<TrackNav[]> {
  cache ??= (async () => {
    const [tracks, topics] = await Promise.all([
      getCollection('tracks'),
      getCollection('topics', visible),
    ]);
    const ids = new Set(tracks.map((t) => t.id));
    for (const topic of topics) {
      const prefix = topic.id.split('/')[0];
      if (prefix !== topic.data.track.id || !ids.has(prefix)) {
        throw new Error(
          `Topic "${topic.id}" lives in folder "${prefix}" but declares track "${topic.data.track.id}".`,
        );
      }
    }
    return tracks
      .sort((a, b) => a.data.order - b.data.order)
      .map((track) => ({
        track,
        topics: topics
          .filter((t) => t.data.track.id === track.id)
          .sort((a, b) => a.data.order - b.data.order || a.data.title.localeCompare(b.data.title)),
      }));
  })();
  return cache;
}

export async function getFlatTopics(): Promise<Topic[]> {
  return (await getNav()).flatMap((n) => n.topics);
}

/** Previous/next topic across the whole guide (crosses track boundaries). */
export async function getNeighbours(id: string) {
  const flat = await getFlatTopics();
  const i = flat.findIndex((t) => t.id === id);
  return {
    prev: i > 0 ? flat[i - 1] : undefined,
    next: i >= 0 && i < flat.length - 1 ? flat[i + 1] : undefined,
  };
}

export async function resolveTopics(refs: { id: string }[]): Promise<Topic[]> {
  const out: Topic[] = [];
  for (const ref of refs) {
    const t = await getEntry('topics', ref.id);
    if (!t) throw new Error(`Unknown topic reference: ${ref.id}`);
    out.push(t);
  }
  return out;
}

export function slugifyCompany(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}
