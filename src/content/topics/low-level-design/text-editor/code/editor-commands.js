export class Editor {
  #undo = []; #redo = [];

  constructor(buffer) { this.buffer = buffer; }

  apply(cmd) {
    cmd.execute(this.buffer);
    this.#undo.push(cmd);
    this.#redo.length = 0;
  }
  undo() {
    const cmd = this.#undo.pop();
    if (!cmd) return false;
    cmd.undo(this.buffer);
    this.#redo.push(cmd);
    return true;
  }
  redo() {
    const cmd = this.#redo.pop();
    if (!cmd) return false;
    cmd.execute(this.buffer);
    this.#undo.push(cmd);
    return true;
  }
}

export const insertText = (pos, text) => ({
  execute: (b) => b.insert(pos, text),
  undo: (b) => b.delete(pos, text.length),
});

export const deleteText = (pos, len) => {
  let removed = '';
  return {
    execute: (b) => { removed = b.delete(pos, len); },
    undo: (b) => b.insert(pos, removed),
  };
};
