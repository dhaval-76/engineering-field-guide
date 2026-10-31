// uses DirNode from the model above
export class FileSystem {
  root = DirNode.root();
  cwd = this.root;
  previous = this.root;

  // Resolve absolute or relative paths, handling "." and "..".
  resolve(path) {
    let cur = path.startsWith('/') ? this.root : this.cwd;
    for (const part of path.split('/')) {
      if (!part || part === '.') continue;
      if (cur.type !== 'dir') throw new Error(`not a directory: ${cur.path}`);
      cur = part === '..' ? cur.parent ?? cur : cur.child(part);
      if (!cur) throw new Error(`no such file or directory: ${path}`);
    }
    return cur;
  }

  // mkdir -p: create every missing directory along the path.
  mkdirs(path) {
    let dir = path.startsWith('/') ? this.root : this.cwd;
    for (const part of path.split('/')) {
      if (!part || part === '.') continue;
      if (part === '..') {
        dir = dir.parent ?? dir;
        continue;
      }
      let next = dir.child(part);
      if (!next) dir.add((next = new DirNode(part)));
      if (next.type !== 'dir') throw new Error(`not a directory: ${next.path}`);
      dir = next;
    }
    return dir;
  }

  cd(path) {
    if (path === '-') {
      [this.cwd, this.previous] = [this.previous, this.cwd];
      return;
    }
    const d = this.resolve(path);
    if (d.type !== 'dir') throw new Error(`not a directory: ${path}`);
    [this.previous, this.cwd] = [this.cwd, d];
  }

  pwd() {
    return this.cwd.path;
  }

  ls(path = '.') {
    const n = this.resolve(path);
    return n.type === 'dir' ? n.children.map((c) => c.name + (c.type === 'dir' ? '/' : '')) : [n.name];
  }
}
