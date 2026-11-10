import socket
import ssl
import time


def days_left(host: str) -> int:
    """Opens a TLS connection and returns how many days the server's certificate has left."""
    context = ssl.create_default_context()  # verifies the chain and the host name
    with socket.create_connection((host, 443), timeout=5) as raw:
        with context.wrap_socket(raw, server_hostname=host) as tls:  # sends SNI
            not_after = ssl.cert_time_to_seconds(tls.getpeercert()["notAfter"])
    return int((not_after - time.time()) // 86400)
