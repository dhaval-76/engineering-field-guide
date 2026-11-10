import net from 'node:net';

/** Sends GET over a plain TCP socket and resolves with the status line. */
function statusLine(host, port, path) {
  return new Promise((resolve, reject) => {
    const socket = net.connect({ host, port }, () => {
      socket.write(
        `GET ${path} HTTP/1.1\r\n` +
          `Host: ${host}\r\n` + // required in HTTP/1.1
          'Connection: close\r\n' +
          '\r\n', // blank line ends the headers
      );
    });
    socket.setTimeout(2000, () => socket.destroy(new Error('timeout')));
    let text = '';
    socket.on('data', (chunk) => {
      text += chunk.toString('latin1');
      const end = text.indexOf('\r\n');
      if (end >= 0) {
        resolve(text.slice(0, end));
        socket.destroy();
      }
    });
    socket.on('error', reject);
  });
}
