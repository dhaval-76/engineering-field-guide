import dgram from 'node:dgram';

/** A DNS question for a name's IPv4 addresses, built by hand. */
function buildQuery(name) {
  const header = Buffer.from([0x12, 0x34, 0x01, 0x00, 0, 1, 0, 0, 0, 0, 0, 0]); // id, RD, 1 question
  const labels = name.split('.').map((l) => Buffer.concat([Buffer.from([l.length]), Buffer.from(l)]));
  return Buffer.concat([header, ...labels, Buffer.from([0, 0, 1, 0, 1])]); // type A, class IN
}

/** Reads the A records (address and remaining TTL) out of a reply. */
function parseReply(r) {
  const answers = r.readUInt16BE(6);
  let at = skipName(r, 12) + 4; // past the echoed question
  const out = [];
  for (let i = 0; i < answers; i++) {
    at = skipName(r, at);
    const type = r.readUInt16BE(at);
    const ttl = r.readUInt32BE(at + 4);
    const length = r.readUInt16BE(at + 8);
    if (type === 1) out.push({ address: [...r.subarray(at + 10, at + 14)].join('.'), ttl });
    at += 10 + length; // type 5 is a CNAME: the A records for its target follow
  }
  return out;
}

/** Names are labels, or a 2-byte pointer (top bits 11) to a name earlier in the packet. */
function skipName(r, at) {
  for (;;) {
    const len = r[at];
    if (len === 0) return at + 1;
    if ((len & 0xc0) === 0xc0) return at + 2;
    at += len + 1;
  }
}

/** One UDP packet out, one back. No reply in time means ask again or give up. */
function lookup(name, resolver) {
  return new Promise((resolve, reject) => {
    const socket = dgram.createSocket('udp4');
    const timer = setTimeout(() => {
      socket.close();
      reject(new Error('no reply'));
    }, 2000);
    socket.on('message', (reply) => {
      clearTimeout(timer);
      socket.close();
      resolve(parseReply(reply));
    });
    socket.send(buildQuery(name), 53, resolver);
  });
}
