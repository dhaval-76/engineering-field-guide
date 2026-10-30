/** Prefix an internal path with the configured base (GitHub Pages serves from /<repo>/). */
export function url(path = ''): string {
  const base = import.meta.env.BASE_URL.replace(/\/$/, '');
  const clean = path.replace(/^\//, '');
  return clean ? `${base}/${clean}` : `${base}/`;
}

export const topicUrl = (topicId: string) => url(`${topicId}/`);
export const trackUrl = (trackId: string) => url(`${trackId}/`);
