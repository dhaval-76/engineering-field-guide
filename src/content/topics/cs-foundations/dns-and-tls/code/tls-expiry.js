import tls from 'node:tls';

/** Opens a TLS connection and resolves with how many days the server's certificate has left. */
function daysLeft(host) {
  return new Promise((resolve, reject) => {
    // servername sends SNI; the chain and host name are verified by default
    const socket = tls.connect({ host, port: 443, servername: host }, () => {
      const notAfter = new Date(socket.getPeerCertificate().valid_to);
      socket.end();
      resolve(Math.floor((notAfter - Date.now()) / 86_400_000));
    });
    socket.on('error', reject); // a bad chain or wrong name fails here
  });
}
