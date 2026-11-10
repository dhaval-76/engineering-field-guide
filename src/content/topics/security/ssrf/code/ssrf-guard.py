import ipaddress
import socket
from urllib.parse import urlsplit


def safe_destination(url: str) -> str:
    """Resolves a user-supplied URL once and returns the address to connect to, or raises if it
    points inside our network. Connect to this address (sending the Host header), not to the
    name again: a second lookup could return something else (DNS rebinding)."""
    parts = urlsplit(url)
    if parts.scheme not in ("http", "https"):
        raise ValueError(f"scheme not allowed: {parts.scheme}")  # file:, gopher:, ...
    if not parts.hostname:
        raise ValueError("no host")
    infos = socket.getaddrinfo(parts.hostname, parts.port or 443, type=socket.SOCK_STREAM)
    addresses = [info[4][0] for info in infos]
    for address in addresses:  # every answer must be safe, not just the first
        if is_internal(ipaddress.ip_address(address)):
            raise ValueError(f"internal address: {address}")
    return addresses[0]


def is_internal(ip) -> bool:
    if ip.version == 6 and ip.ipv4_mapped:  # an IPv4 address in disguise
        ip = ip.ipv4_mapped
    # is_global is False for private, loopback, link-local (cloud metadata), shared and reserved
    return not ip.is_global or ip.is_multicast
