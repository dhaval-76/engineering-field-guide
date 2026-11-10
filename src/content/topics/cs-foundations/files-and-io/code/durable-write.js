import fs from 'node:fs';
import path from 'node:path';

/** Replaces a file so that after a crash it holds the old contents or the new, never half. */
function writeDurably(target, data) {
  const temp = `${target}.tmp`;
  const fd = fs.openSync(temp, 'w');
  try {
    fs.writeFileSync(fd, data); // loops over partial writes for us
    fs.fsyncSync(fd); // until now the data may only be in the page cache
  } finally {
    fs.closeSync(fd);
  }
  fs.renameSync(temp, target); // atomic: readers see the old file or the new one
  const dir = fs.openSync(path.dirname(target), 'r');
  try {
    fs.fsyncSync(dir); // fsync the directory, or the rename itself may be lost
  } finally {
    fs.closeSync(dir);
  }
}
