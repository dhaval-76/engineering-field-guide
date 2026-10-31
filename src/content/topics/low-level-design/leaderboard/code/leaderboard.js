// In-memory leaderboard on a sorted array: binary search to find positions. Inserts shift
// elements (O(n)) — fine for thousands of players; use Redis sorted sets beyond that.
const before = (a, b) =>
  b.score - a.score ||
  a.reachedAt - b.reachedAt ||
  (a.player < b.player ? -1 : a.player > b.player ? 1 : 0);

export class Leaderboard {
  #byPlayer = new Map();
  #ranking = []; // sorted by `before`
  #clock = 0;

  #indexOf(entry) {
    let lo = 0;
    let hi = this.#ranking.length;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (before(this.#ranking[mid], entry) < 0) lo = mid + 1;
      else hi = mid;
    }
    return lo;
  }

  addScore(player, delta) {
    const old = this.#byPlayer.get(player);
    if (old) this.#ranking.splice(this.#indexOf(old), 1);
    const entry = { player, score: (old?.score ?? 0) + delta, reachedAt: ++this.#clock };
    this.#byPlayer.set(player, entry);
    this.#ranking.splice(this.#indexOf(entry), 0, entry);
    return entry.score;
  }

  top(k) {
    return this.#ranking.slice(0, k).map(({ player, score }) => [player, score]);
  }

  rank(player) {
    const e = this.#byPlayer.get(player);
    return e ? this.#indexOf(e) + 1 : -1; // O(log n)
  }
}
