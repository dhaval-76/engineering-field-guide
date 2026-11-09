// For non-negative values up to Number.MAX_SAFE_INTEGER.
/** Protobuf's variable-length integer: 7 bits per byte, low bits first, top bit means "more". */
function writeVarint(out, value) {
  while (value > 127) {
    out.push((value % 128) | 128);
    value = Math.floor(value / 128);
  }
  out.push(value);
}

/** Every field starts with its number and wire type. Names are never sent. */
function writeTag(out, fieldNumber, wireType) {
  writeVarint(out, fieldNumber * 8 + wireType);
}

function writeInt(out, fieldNumber, value) {
  writeTag(out, fieldNumber, 0); // wire type 0: varint
  writeVarint(out, value);
}

function writeString(out, fieldNumber, value) {
  const bytes = new TextEncoder().encode(value);
  writeTag(out, fieldNumber, 2); // wire type 2: length-delimited
  writeVarint(out, bytes.length);
  out.push(...bytes);
}

/** Returns [value, offset of the next byte]. */
function readVarint(data, at) {
  let value = 0;
  for (let scale = 1; ; scale *= 128) {
    const byte = data[at++];
    value += (byte & 127) * scale;
    if ((byte & 128) === 0) return [value, at];
  }
}

/** gRPC's frame: 1 byte "compressed?" flag, 4 bytes big-endian length, then the message. */
function grpcFrame(message) {
  const frame = new Uint8Array(5 + message.length);
  new DataView(frame.buffer).setUint32(1, message.length);
  frame.set(message, 5);
  return frame;
}
