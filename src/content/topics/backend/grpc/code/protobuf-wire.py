import struct


def write_varint(out, value):
    """Protobuf's variable-length integer: 7 bits per byte, low bits first, top bit means more."""
    while value > 127:
        out.append((value & 0x7F) | 0x80)
        value >>= 7
    out.append(value)


def write_tag(out, field_number, wire_type):
    """Every field starts with its number and wire type. Names are never sent."""
    write_varint(out, (field_number << 3) | wire_type)


def write_int(out, field_number, value):
    write_tag(out, field_number, 0)  # wire type 0: varint
    write_varint(out, value)


def write_string(out, field_number, value):
    data = value.encode()
    write_tag(out, field_number, 2)  # wire type 2: length-delimited
    write_varint(out, len(data))
    out.extend(data)


def read_varint(data, at):
    """Returns (value, offset of the next byte)."""
    value = shift = 0
    while True:
        byte = data[at]
        at += 1
        value |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return value, at
        shift += 7


def grpc_frame(message):
    """gRPC's frame: 1 byte "compressed?" flag, 4 bytes big-endian length, then the message."""
    return struct.pack(">BI", 0, len(message)) + bytes(message)
