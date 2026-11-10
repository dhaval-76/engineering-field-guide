import { lookup } from 'node:dns/promises';
import net from 'node:net';

const internal = new net.BlockList();
for (const [address, prefix] of [
  ['0.0.0.0', 8], ['10.0.0.0', 8], ['100.64.0.0', 10], ['127.0.0.0', 8],
  ['169.254.0.0', 16], // link-local: cloud metadata
  ['172.16.0.0', 12], ['192.168.0.0', 16], ['224.0.0.0', 3],
]) internal.addSubnet(address, prefix, 'ipv4'); // also matches IPv4-mapped IPv6 (::ffff:7f00:1)
for (const [address, prefix] of [
  ['::', 127], // unspecified and loopback
  ['fc00::', 7], ['fe80::', 10], ['ff00::', 8],
]) internal.addSubnet(address, prefix, 'ipv6');

/**
 * Resolves a user-supplied URL once and returns the address to connect to, or throws if it
 * points inside our network. Connect to this address (sending the Host header), not to the
 * name again: a second lookup could return something else (DNS rebinding).
 */
async function safeDestination(url) {
  const u = new URL(url); // also normalises tricks like http://2130706433/
  if (u.protocol !== 'http:' && u.protocol !== 'https:') {
    throw new Error(`scheme not allowed: ${u.protocol}`);
  }
  const host = u.hostname.replace(/^\[|\]$/g, '');
  const addresses = await lookup(host, { all: true });
  for (const { address, family } of addresses) { // every answer must be safe
    if (internal.check(address, family === 6 ? 'ipv6' : 'ipv4')) {
      throw new Error(`internal address: ${address}`);
    }
  }
  return addresses[0].address;
}
