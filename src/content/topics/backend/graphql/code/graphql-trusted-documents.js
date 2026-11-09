import { createHash } from "node:crypto";

/** Only operations registered at build time can run. Clients send a hash, not a query. */
export class TrustedDocuments {
  #byHash = new Map();

  /** Called from the client's build, for each operation it contains. */
  register(document) {
    const hash = createHash("sha256").update(document).digest("hex");
    this.#byHash.set(hash, document);
    return hash;
  }

  /** Undefined means reject: this query did not come from a build of our own app. */
  lookup(hash) {
    return this.#byHash.get(hash);
  }
}
