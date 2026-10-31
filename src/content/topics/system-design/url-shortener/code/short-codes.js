const ALPHABET = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
const SPACE = 62n ** 7n; // 3,521,614,606,208 seven-character codes
// coprime with 62^7 → multiplication mod 62^7 is a bijection
const MULTIPLIER = 1_580_030_173n;

// Fixed-width base62 so every code has 7 characters. BigInt: ids exceed 2^53 safely.
export function encode(n) {
  let x = BigInt(n),
    out = '';
  for (let i = 0; i < 7; i++) {
    out = ALPHABET[Number(x % 62n)] + out;
    x /= 62n;
  }
  return out;
}

export function decode(code) {
  let n = 0n;
  for (const c of code) {
    const i = ALPHABET.indexOf(c);
    if (i < 0) throw new Error(`not base62: ${c}`);
    n = n * 62n + BigInt(i);
  }
  return n;
}

// Spread consecutive ids across the code space so codes aren't sequential (not
// encryption).
export function scrambled(id) {
  const n = BigInt(id);
  if (n < 0n || n >= SPACE) throw new Error('id out of range');
  return encode((n * MULTIPLIER) % SPACE);
}

// Each instance reserves 10,000 ids at a time, so creating a link needs no shared call
// per link.
export class IdRange {
  #next = 0n;
  #end = 0n;
  constructor(pool) {
    this.pool = pool;
  }
  async nextId() {
    if (this.#next === this.#end) {
      const { rows } = await this.pool.query(
        `UPDATE id_blocks SET next_value = next_value + 10000 WHERE name = 'links' RETURNING next_value - 10000 AS start`,
      );
      this.#next = BigInt(rows[0].start);
      this.#end = this.#next + 10_000n;
    }
    return this.#next++;
  }
}
// (Two concurrent nextId() calls at a block boundary could both fetch a block — wasteful,
// never duplicate.)
