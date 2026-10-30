const DIRS = { UP: [-1, 0], DOWN: [1, 0], LEFT: [0, -1], RIGHT: [0, 1] };
const key = ([r, c]) => `${r},${c}`;

export class SnakeGame {
  constructor({ rows, cols, foodSequence }) {
    Object.assign(this, { rows, cols, food: [...foodSequence], score: 0, over: false, dir: 'RIGHT' });
    this.body = [[0, 0]];                    // body[0] = head
    this.occupied = new Set([key([0, 0])]);
  }

  move(dirName) {
    if (this.over) return -1;
    const [dr, dc] = DIRS[dirName];
    const [hr, hc] = this.body[0];
    const next = [hr + dr, hc + dc];
    if (next[0] < 0 || next[0] >= this.rows || next[1] < 0 || next[1] >= this.cols) return this.#end();

    const eats = this.food.length && key(this.food[0]) === key(next);
    if (!eats) this.occupied.delete(key(this.body.pop()));   // tail leaves first
    if (this.occupied.has(key(next))) return this.#end();
    this.body.unshift(next);
    this.occupied.add(key(next));
    if (eats) { this.food.shift(); this.score++; }
    return this.score;
  }

  #end() { this.over = true; return -1; }
}
// (body.unshift is O(n); a ring buffer or linked deque gives O(1) for very long snakes.)
