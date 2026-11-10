import struct


def encode(message: str) -> bytes:
    """Prefixes each message with its length as 4 bytes, so the receiver knows where it ends."""
    body = message.encode("utf-8")
    return struct.pack(">I", len(body)) + body


class FrameDecoder:
    """Collects bytes as TCP delivers them and returns each message once it is complete."""

    def __init__(self) -> None:
        self._pending = b""

    def feed(self, chunk: bytes) -> list[str]:
        self._pending += chunk  # a read may hold half a message, or three of them
        messages = []
        while len(self._pending) >= 4:
            (length,) = struct.unpack(">I", self._pending[:4])
            if len(self._pending) < 4 + length:
                break  # body not fully here
            messages.append(self._pending[4 : 4 + length].decode("utf-8"))
            self._pending = self._pending[4 + length :]
        return messages
