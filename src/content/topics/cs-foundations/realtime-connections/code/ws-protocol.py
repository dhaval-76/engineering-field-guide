import base64
import hashlib
import struct

GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"  # fixed by RFC 6455


def accept_key(sec_websocket_key: str) -> str:
    """The server's Sec-WebSocket-Accept: proof it understood this upgrade, not a cached reply."""
    sha1 = hashlib.sha1((sec_websocket_key + GUID).encode("ascii")).digest()
    return base64.b64encode(sha1).decode("ascii")


def text_frame(text: str, mask: bytes | None = None) -> bytes:
    """One complete text frame. Clients must mask with 4 random bytes; servers must not."""
    payload = text.encode("utf-8")
    mask_bit = 0x80 if mask else 0
    if len(payload) < 126:
        header = bytes([0x81, mask_bit | len(payload)])  # FIN + opcode 1 (text)
    elif len(payload) < 65536:
        header = bytes([0x81, mask_bit | 126]) + struct.pack(">H", len(payload))
    else:
        header = bytes([0x81, mask_bit | 127]) + struct.pack(">Q", len(payload))
    if not mask:
        return header + payload
    return header + mask + bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
