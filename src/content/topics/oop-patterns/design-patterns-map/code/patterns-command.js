// Command: an action as an object — can be queued, logged, retried or undone.
const insertText = (doc, pos, text) => ({
  execute: () => doc.insert(pos, text),
  undo: () => doc.delete(pos, text.length),
});

class Editor {
  #undo = [];
  #redo = [];
  run(cmd) { cmd.execute(); this.#undo.push(cmd); this.#redo.length = 0; }
  undo() { const c = this.#undo.pop(); if (c) { c.undo(); this.#redo.push(c); } }
  redo() { const c = this.#redo.pop(); if (c) { c.execute(); this.#undo.push(c); } }
}
