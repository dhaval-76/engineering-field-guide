const Risk = Object.freeze({
  READ_PUBLIC: "read_public",
  READ_PRIVATE: "read_private",
  WRITE: "write",
  SENDS_OUT: "sends_out",
});

/**
 * Decides in code whether the model may use a tool. The model only proposes; it cannot talk its
 * way past this, because this class never reads its reasoning.
 */
class ToolGate {
  #allowed;
  #sawUntrusted = false;
  #sawPrivate = false;

  constructor(allowedTools) {
    this.#allowed = new Set(allowedTools);
  }

  /** Call for every tool result that an outsider could have influenced. */
  recordUntrustedContent() {
    this.#sawUntrusted = true;
  }

  authorize(tool, risk) {
    if (!this.#allowed.has(tool)) return "deny";
    if (risk === Risk.READ_PUBLIC) return "allow";
    if (risk === Risk.READ_PRIVATE) {
      this.#sawPrivate = true;
      return "allow";
    }
    // Private data + untrusted content + a way out is how data gets stolen.
    if (risk === Risk.SENDS_OUT && this.#sawUntrusted && this.#sawPrivate) return "deny";
    return this.#sawUntrusted ? "ask_human" : "allow";
  }
}
