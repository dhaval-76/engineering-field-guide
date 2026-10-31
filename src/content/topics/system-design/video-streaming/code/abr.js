// Adaptive bitrate selection, run before each segment download.
export class AbrController {
  #estimate = -1; // kbps, exponentially weighted
  #current = 0;

  constructor(ladder) {
    this.ladder = ladder; // [{ name, kbps }] ascending
  }

  onSegmentDownloaded(bits, seconds) {
    const sample = bits / 1000 / seconds;
    this.#estimate = this.#estimate < 0 ? sample : 0.7 * this.#estimate + 0.3 * sample;
  }

  next(bufferSeconds) {
    if (this.#estimate < 0 || bufferSeconds < 5) return this.ladder[(this.#current = 0)];
    const budget = 0.8 * this.#estimate; // 20% headroom
    let best = 0;
    this.ladder.forEach((r, i) => {
      if (r.kbps <= budget) best = i;
    });
    if (best > this.#current) best = bufferSeconds > 15 ? this.#current + 1 : this.#current; // climb one rung
    this.#current = best;
    return this.ladder[best];
  }
}
