// Node.js: a single-threaded event loop over non-blocking sockets (libuv uses
// epoll/kqueue/IOCP).
import net from 'node:net';

net.createServer((socket) => {
  socket.on('data', (chunk) => {
    // never block here: CPU-heavy work goes to worker_threads
    socket.write(chunk);
  });
}).listen(8080);
// One thread serves thousands of connections because it only runs when a socket is ready.
