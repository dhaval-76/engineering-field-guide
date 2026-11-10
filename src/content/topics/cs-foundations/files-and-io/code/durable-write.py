import os


def write_durably(target: str, data: bytes) -> None:
    """Replaces a file so that after a crash it holds the old contents or the new, never half."""
    temp = target + ".tmp"
    fd = os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o644)
    try:
        view = memoryview(data)
        while view:
            view = view[os.write(fd, view) :]  # a write may be partial
        os.fsync(fd)  # until now the data may only be in the page cache
    finally:
        os.close(fd)
    os.replace(temp, target)  # atomic: readers see the old file or the new one
    dir_fd = os.open(os.path.dirname(target) or ".", os.O_RDONLY)
    try:
        os.fsync(dir_fd)  # fsync the directory, or the rename itself may be lost
    finally:
        os.close(dir_fd)
