// In-memory log-based pub/sub: each topic is an append-only log; each consumer group has
// its own offset.
export class Broker {
  #topics = new Map(); // name -> { log: [], baseOffset, committed: Map(group -> offset) }

  #topic(name) {
    if (!this.#topics.has(name))
      this.#topics.set(name, { log: [], baseOffset: 0, committed: new Map() });
    return this.#topics.get(name);
  }

  publish(topicName, key, payload) {
    const t = this.#topic(topicName);
    const offset = t.baseOffset + t.log.length;
    t.log.push({ offset, key, payload });
    return offset;
  }

  subscribe(topicName, group, fromBeginning = false) {
    const t = this.#topic(topicName);
    if (!t.committed.has(group))
      t.committed.set(group, fromBeginning ? t.baseOffset : t.baseOffset + t.log.length);
  }

  poll(topicName, group, max) {
    const t = this.#topic(topicName);
    if (!t.committed.has(group)) throw new Error(`not subscribed: ${group}`);
    const start = t.committed.get(group) - t.baseOffset;
    return t.log.slice(start, start + max);
  }

  commit(topicName, group, nextOffset) {
    const t = this.#topic(topicName);
    t.committed.set(group, Math.max(t.committed.get(group) ?? 0, nextOffset));
  }

  trim(topicName) {
    const t = this.#topic(topicName);
    const end = t.baseOffset + t.log.length;
    const minCommitted = Math.min(end, ...t.committed.values());
    const drop = minCommitted - t.baseOffset;
    t.log.splice(0, drop);
    t.baseOffset = minCommitted;
    return drop;
  }
}
