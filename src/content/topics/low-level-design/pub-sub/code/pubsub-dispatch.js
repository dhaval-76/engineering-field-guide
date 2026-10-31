// uses Broker from above
// Push delivery for one group: poll, hand to the handler, retry with backoff, dead-letter, then commit.
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export class Dispatcher {
  running = true;

  constructor(broker, topic, group, handler, maxAttempts = 5) {
    Object.assign(this, { broker, topic, group, handler, maxAttempts });
    broker.subscribe(topic, group, false);
  }

  async run() {
    while (this.running) {
      const batch = this.broker.poll(this.topic, this.group, 100);
      if (!batch.length) {
        await sleep(50);
        continue;
      }
      for (const m of batch) {
        await this.#deliver(m);
        this.broker.commit(this.topic, this.group, m.offset + 1); // commit after handling: at-least-once
      }
    }
  }

  async #deliver(m) {
    for (let attempt = 1; ; attempt++) {
      try {
        await this.handler(m);
        return;
      } catch {
        if (attempt === this.maxAttempts) {
          this.broker.publish(`${this.topic}.dlq`, m.key, m.payload); // poison message: don't block the topic
          return;
        }
        await sleep(100 * 2 ** attempt);                             // exponential backoff
      }
    }
  }

  stop() {
    this.running = false;
  }
}
