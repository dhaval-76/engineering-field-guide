// Relationship tuples (Zanzibar-style), e.g. in OpenFGA / SpiceDB:
//   folder:roadmap#editor@user:asha
//   document:q3-plan#parent@folder:roadmap
// Model: a document's editors include editors of its parent folder.
import { LRUCache } from 'lru-cache';

export class DocumentAccess {
  // short TTL: revocations take effect within seconds
  #recent = new LRUCache({ max: 100_000, ttl: 5000 });

  constructor(permissions) {
    this.permissions = permissions; // OpenFGA/SpiceDB client, a replicated service
  }

  async requireEdit(userId, docId) {
    const key = `${userId}|edit|${docId}`;
    let allowed = this.#recent.get(key);
    if (allowed === undefined) {
      allowed = await this.permissions.check(`user:${userId}`, 'editor', `document:${docId}`);
      this.#recent.set(key, allowed);
    }
    if (!allowed) throw Object.assign(new Error(`document ${docId}`), { status: 403 });
  }

  // List pages: one batched call, not one check per row.
  async visible(userId, candidates) {
    const allowed = new Set(await this.permissions.batchCheck(`user:${userId}`, 'viewer', candidates.map((d) => `document:${d.id}`)));
    return candidates.filter((d) => allowed.has(`document:${d.id}`));
  }

  share(docId, withUserId) {
    // a new tuple
    return this.permissions.write(`document:${docId}`, 'editor', `user:${withUserId}`);
  }
}
