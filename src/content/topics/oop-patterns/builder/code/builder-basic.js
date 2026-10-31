// Idiomatic JS: an options object gives named arguments and defaults for free.
export function createUser({ name, email, age = 0, phone = null, role = 'USER', verified = false }) {
  if (!name?.trim()) throw new Error('name is required');
  if (!email?.includes('@')) throw new Error('valid email is required');
  return Object.freeze({ name, email, age, phone, role, verified });
}

const u = createUser({ name: 'Asha', email: 'asha@example.com', role: 'ADMIN', verified: true });

// A fluent builder is still useful when steps accumulate (e.g. query or request
// builders):
export class RequestBuilder {
  #method = 'GET'; #headers = {}; #body;
  constructor(url) { this.url = url; }
  method(m) { this.#method = m; return this; }
  header(k, v) { this.#headers[k] = v; return this; }
  body(b) { this.#body = b; return this; }
  build() {
    if (this.#body !== undefined && ['GET', 'HEAD'].includes(this.#method)) {
      throw new Error(`${this.#method} requests cannot have a body`);
    }
    return Object.freeze({
      url: this.url,
      method: this.#method,
      headers: { ...this.#headers },
      body: this.#body,
    });
  }
}
