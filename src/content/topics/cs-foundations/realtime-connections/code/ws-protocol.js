import { createHash } from 'node:crypto';

const GUID = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11'; // fixed by RFC 6455

/** The server's Sec-WebSocket-Accept: proof it understood this upgrade, not a cached reply. */
function acceptKey(secWebSocketKey) {
  return createHash('sha1').update(secWebSocketKey + GUID).digest('base64');
}

/** One complete text frame. Clients must mask with 4 random bytes; servers must not. */
function textFrame(text, mask) {
  const payload = Buffer.from(text, 'utf8');
  const maskBit = mask ? 0x80 : 0;
  let header;
  if (payload.length < 126) {
    header = Buffer.from([0x81, maskBit | payload.length]); // FIN + opcode 1 (text)
  } else if (payload.length < 65536) {
    header = Buffer.alloc(4); // length follows in 2 bytes
    header.writeUInt8(0x81, 0);
    header.writeUInt8(maskBit | 126, 1);
    header.writeUInt16BE(payload.length, 2);
  } else {
    header = Buffer.alloc(10); // length follows in 8 bytes
    header.writeUInt8(0x81, 0);
    header.writeUInt8(maskBit | 127, 1);
    header.writeBigUInt64BE(BigInt(payload.length), 2);
  }
  if (!mask) return Buffer.concat([header, payload]);
  const masked = payload.map((b, i) => b ^ mask[i % 4]);
  return Buffer.concat([header, Buffer.from(mask), masked]);
}
