/** Prefixes each message with its length as 4 bytes, so the receiver knows where it ends. */
function encode(message) {
  const body = Buffer.from(message, 'utf8');
  const header = Buffer.alloc(4);
  header.writeUInt32BE(body.length);
  return Buffer.concat([header, body]);
}

/** Collects bytes as TCP delivers them and returns each message once it is complete. */
class FrameDecoder {
  #pending = Buffer.alloc(0);

  feed(chunk) {
    this.#pending = Buffer.concat([this.#pending, chunk]); // half a message, or three
    const messages = [];
    while (this.#pending.length >= 4) {
      const length = this.#pending.readUInt32BE(0);
      if (this.#pending.length < 4 + length) break; // body not fully here
      messages.push(this.#pending.subarray(4, 4 + length).toString('utf8'));
      this.#pending = this.#pending.subarray(4 + length);
    }
    return messages;
  }
}
