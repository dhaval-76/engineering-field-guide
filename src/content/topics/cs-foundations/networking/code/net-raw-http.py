import socket


def status_line(host: str, port: int, path: str) -> str:
    """Sends GET over a plain TCP socket and returns the status line."""
    with socket.create_connection((host, port), timeout=2) as sock:  # TCP handshake
        request = (
            f"GET {path} HTTP/1.1\r\n"
            f"Host: {host}\r\n"  # required in HTTP/1.1
            "Connection: close\r\n"
            "\r\n"  # blank line ends the headers
        )
        sock.sendall(request.encode("ascii"))
        with sock.makefile("rb") as response:
            return response.readline().decode("latin-1").rstrip("\r\n")
