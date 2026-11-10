import fs from 'node:fs';

/**
 * Reads a secret from a file mounted by the platform (a Kubernetes secret, a Vault agent),
 * and picks up a rotated value without a restart. Never logs or returns it anywhere else.
 */
class FileSecret {
  #path;
  #seen = -1;
  #value = '';

  constructor(path) {
    this.#path = path;
  }

  get() {
    const modified = fs.statSync(this.#path).mtimeMs;
    if (modified !== this.#seen) { // the platform replaced the file: read the new value
      this.#value = fs.readFileSync(this.#path, 'utf8').trim();
      this.#seen = modified;
    }
    return this.#value;
  }

  toJSON() {
    return '[secret hidden]'; // safe if it ends up in a log
  }
}
