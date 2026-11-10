import os
import socket


def send_file(path: str, sock: socket.socket) -> int:
    """Sends a file to a socket with sendfile: the kernel moves the bytes, Python never sees them."""
    with open(path, "rb") as f:
        size = os.fstat(f.fileno()).st_size
        sent = 0
        while sent < size:  # may send less than asked
            sent += os.sendfile(sock.fileno(), f.fileno(), sent, size - sent)
        return sent
