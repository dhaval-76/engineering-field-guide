import { createHash } from 'node:crypto';

/** Serves a cacheable resource, answering 304 Not Modified when the client's copy is current. */
function serve(body, ifNoneMatch) {
  const etag = `"${createHash('sha256').update(body).digest('hex').slice(0, 16)}"`;
  const headers = {
    ETag: etag,
    // reuse for 60 s without asking; afterwards revalidate with If-None-Match
    'Cache-Control': 'max-age=60',
  };
  if (ifNoneMatch && matches(ifNoneMatch, etag)) {
    return { status: 304, headers, body: '' }; // no body: the client already has it
  }
  return { status: 200, headers, body };
}

/** If-None-Match may list several tags, or "*"; W/ (weak) tags compare by value. */
function matches(ifNoneMatch, etag) {
  return ifNoneMatch
    .split(',')
    .map((t) => t.trim())
    .some((t) => t === '*' || t.replace(/^W\//, '') === etag);
}
