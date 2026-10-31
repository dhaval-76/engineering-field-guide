// Composite: files and directories share one shape, so traversal code treats them uniformly.
class Node {
  parent = null;
  modified = Date.now();
  constructor(name) {
    this.name = name;
  }
  get path() {
    if (!this.parent) return '/';
    return (this.parent.parent ? this.parent.path : '') + '/' + this.name;
  }
}

export class FileNode extends Node {
  type = 'file';
  #content = new Uint8Array(0);
  get size() {
    return this.#content.length;
  }
  write(bytes) {
    this.#content = Uint8Array.from(bytes);
    this.modified = Date.now();
  }
  append(bytes) {
    const next = new Uint8Array(this.#content.length + bytes.length);
    next.set(this.#content);
    next.set(bytes, this.#content.length);
    this.#content = next;
    this.modified = Date.now();
  }
}

export class DirNode extends Node {
  type = 'dir';
  #children = new Map();
  static root() {
    return new DirNode(''); // the only nameless node
  }
  get size() {
    return this.children.reduce((sum, c) => sum + c.size, 0); // recursive by composition
  }
  get children() {
    return [...this.#children.values()].sort((a, b) => (a.name < b.name ? -1 : 1)); // sorted for ls
  }
  child(name) {
    return this.#children.get(name) ?? null;
  }
  add(node) {
    if (!node.name || node.name.includes('/') || node.name === '.' || node.name === '..') throw new Error(`bad name: ${node.name}`);
    if (this.#children.has(node.name)) throw new Error(`${node.name} already exists`);
    this.#children.set(node.name, node);
    node.parent = this;
  }
  remove(name) {
    this.#children.delete(name);
  }
}
