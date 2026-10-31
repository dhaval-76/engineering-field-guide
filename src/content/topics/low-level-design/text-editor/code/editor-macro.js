// A composite command: one undo step for many actions (paste-with-format, replace-all, macros).
export const composite = (steps) => ({
  execute: (b) => steps.forEach((s) => s.execute(b)),
  undo: (b) => [...steps].reverse().forEach((s) => s.undo(b)), // reverse order
});

// Macro recording: capture commands while recording, replay them later as one composite.
export class MacroRecorder {
  #recording = null;
  start() {
    this.#recording = [];
  }
  capture(cmd) {
    this.#recording?.push(cmd);
  }
  stop() {
    const macro = composite(this.#recording ?? []);
    this.#recording = null;
    return macro;
  }
}
