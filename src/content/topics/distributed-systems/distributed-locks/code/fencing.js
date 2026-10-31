// Lock service that hands out a strictly increasing fencing token with every lease.
export class LeaseService {
  #nextToken = 1;
  #holder = null;
  #expiresAt = 0;

  acquire(client, now, ttl) {
    if (this.#holder !== null && now < this.#expiresAt && this.#holder !== client) return null;
    this.#holder = client;
    this.#expiresAt = now + ttl;
    return this.#nextToken++;
  }
}

// The protected resource rejects writes carrying an older token than one it has already seen.
export class FencedStorage {
  #highest = new Map();
  #data = new Map();

  write(key, value, token) {
    if (token < (this.#highest.get(key) ?? 0)) return false; // stale holder woke up late
    this.#highest.set(key, token);
    this.#data.set(key, value);
    return true;
  }

  read(key) {
    return this.#data.get(key);
  }
}
