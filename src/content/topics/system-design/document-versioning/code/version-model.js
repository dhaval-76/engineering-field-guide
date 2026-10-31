// Content-addressed versions: full snapshots every N saves, deltas in between; head moves with a CAS.
import { createHash } from 'node:crypto';
const SNAPSHOT_EVERY = 10;
const sha256 = (buf) => createHash('sha256').update(buf).digest('hex');

export async function save(c, { pageId, expectedHead, content, authorId, message }) {
  const bytes = Buffer.from(content, 'utf8');
  const hash = sha256(bytes);
  if (!(await blobs.exists(hash))) {                     // identical content (or a restore) stores nothing
    const parent = await blobs.get((await versions.get(expectedHead)).contentHash);
    if (parent.chainLength + 1 >= SNAPSHOT_EVERY) await blobs.putFull(hash, bytes);
    else await blobs.putDelta(hash, parent.hash, delta(await read(parent.hash), bytes), parent.chainLength + 1);
  }
  const id = await versions.insert(c, { pageId, parent: expectedHead, hash, authorId, message });
  const moved = await c.query('UPDATE pages SET head_id = $1 WHERE id = $2 AND head_id = $3', [id, pageId, expectedHead]);
  if (moved.rowCount === 0) throw Object.assign(new Error('concurrent edit — rebase or merge'), { status: 409 });
  return id;
}

// Rebuild content: walk back to the nearest full snapshot, then apply deltas forwards.
export async function read(hash) {
  const chain = [];
  let b = await blobs.get(hash);
  while (b.isDelta) { chain.push(b); b = await blobs.get(b.baseHash); }
  let content = b.data;
  while (chain.length) content = applyDelta(content, chain.pop().data);
  return content.toString('utf8');
}
