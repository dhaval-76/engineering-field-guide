import socket
import struct


def build_query(name: str) -> bytes:
    """A DNS question for a name's IPv4 addresses, built by hand."""
    header = struct.pack(">HHHHHH", 0x1234, 0x0100, 1, 0, 0, 0)  # id, RD, 1 question
    qname = b"".join(bytes([len(l)]) + l.encode() for l in name.split(".")) + b"\0"
    return header + qname + struct.pack(">HH", 1, 1)  # type A, class IN


def parse_reply(r: bytes) -> list[tuple[str, int]]:
    """Reads the A records (address and remaining TTL) out of a reply."""
    (answers,) = struct.unpack_from(">H", r, 6)
    at = skip_name(r, 12) + 4  # past the echoed question
    out = []
    for _ in range(answers):
        at = skip_name(r, at)
        rtype, _cls, ttl, length = struct.unpack_from(">HHIH", r, at)
        if rtype == 1:
            out.append((socket.inet_ntoa(r[at + 10 : at + 14]), ttl))
        at += 10 + length  # type 5 is a CNAME: the A records for its target follow
    return out


def skip_name(r: bytes, at: int) -> int:
    """Names are labels, or a 2-byte pointer (top bits 11) to a name earlier in the packet."""
    while True:
        length = r[at]
        if length == 0:
            return at + 1
        if length & 0xC0 == 0xC0:
            return at + 2
        at += length + 1


def lookup(name: str, resolver: str) -> list[tuple[str, int]]:
    """One UDP packet out, one back. No reply in time means ask again or give up."""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        sock.settimeout(2)
        sock.sendto(build_query(name), (resolver, 53))
        reply, _ = sock.recvfrom(512)
    return parse_reply(reply)
